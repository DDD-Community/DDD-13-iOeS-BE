#!/usr/bin/env bash
# Usage: bash /path/to/deploy/maintenance/scripts/cleanup-disk.sh [--dry-run]
# EC2 디스크 자동 관리: 사용률 임계치 초과 시 docker 찌꺼기(오래된 앱 이미지/dangling/빌드캐시/중지 컨테이너) 정리
# - 운영 컨테이너(photo-app/postgres/redis)와 named volume 은 어떤 경우에도 건드리지 않는다.
# - Cron: 매일 새벽 4시 (setup-cron.sh 로 등록)
# - 로그: JSON 한 줄(level/type=maintenance) → Promtail(job=maintenance) → Loki/Grafana

set -euo pipefail

# ─── 설정 로드 (스크립트 전용 .env, 없으면 기본값) ───────────────
ENV_FILE="$(dirname "$0")/../.env"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck source=../.env
  source "$ENV_FILE"
fi

DISK_THRESHOLD_PERCENT="${DISK_THRESHOLD_PERCENT:-80}"
APP_IMAGE_KEEP_COUNT="${APP_IMAGE_KEEP_COUNT:-3}"
APP_IMAGE_REPO="${APP_IMAGE_REPO:-ghcr.io/ddd-community/ddd-13-ioes-be}"
TARGET_MOUNT="${TARGET_MOUNT:-/}"
LATEST_TAG="${APP_IMAGE_LATEST_TAG:-prod-latest}"
LOCK_FILE="${CLEANUP_LOCK_FILE:-/tmp/cleanup-disk.lock}"
# 반드시 running 상태여야 하는 운영 컨테이너 (하나라도 죽어있으면 정리 중단)
PROTECTED_CONTAINERS="${PROTECTED_CONTAINERS:-photo-app photo-postgres photo-redis}"

DRY_RUN="${DRY_RUN:-0}"
if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=1
fi

log() {
  local level="$1"
  local message="$2"
  echo "{\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"level\":\"${level}\",\"type\":\"maintenance\",\"message\":\"${message}\"}"
}

disk_used_percent() {
  df -P "$TARGET_MOUNT" | awk 'NR==2 {gsub("%","",$5); print $5}'
}

disk_avail_mb() {
  df -Pm "$TARGET_MOUNT" | awk 'NR==2 {print $4}'
}

# ─── 동시 실행 방지 (cron 겹침 방지) ─────────────────────────────
exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  log "INFO" "이미 정리 작업이 실행 중이므로 스킵합니다 (lock=${LOCK_FILE})"
  exit 0
fi

if [[ "$DRY_RUN" == "1" ]]; then
  log "INFO" "DRY-RUN 모드: 실제 삭제 없이 대상만 출력합니다"
fi

# ─── 운영 컨테이너 상태 가드 (배포/장애 중 정리 방지) ────────────
running_image_ids() {
  local name id
  for name in $PROTECTED_CONTAINERS; do
    if ! docker inspect "$name" >/dev/null 2>&1; then
      log "ERROR" "운영 컨테이너 미존재: ${name} — 정리를 중단합니다"
      exit 1
    fi
    if [[ "$(docker inspect "$name" --format '{{.State.Running}}')" != "true" ]]; then
      log "ERROR" "운영 컨테이너 미기동: ${name} — 배포/장애 가능성, 정리를 중단합니다"
      exit 1
    fi
    id="$(docker inspect "$name" --format '{{.Image}}')"
    echo "$id"
  done
}

PROTECTED_IMAGE_IDS="$(running_image_ids)"
log "INFO" "운영 컨테이너 정상 기동 확인 완료 (${PROTECTED_CONTAINERS// /, })"

# ─── 디스크 현황 로깅 (정리 안 해도 하트비트 확보) ───────────────
USED_BEFORE="$(disk_used_percent)"
AVAIL_BEFORE_MB="$(disk_avail_mb)"
log "INFO" "디스크 현황: 사용률=${USED_BEFORE}%, 가용=${AVAIL_BEFORE_MB}MB, 임계치=${DISK_THRESHOLD_PERCENT}% (mount=${TARGET_MOUNT})"

if [[ "$USED_BEFORE" -lt "$DISK_THRESHOLD_PERCENT" ]]; then
  log "INFO" "임계치 미만이므로 정리 불필요"
  exit 0
fi

log "INFO" "임계치 초과 — 안전 정리를 시작합니다"

# ─── 삭제 대상 앱 이미지 산출 (현재 실행 이미지 + prod-latest + 최신 N개 보존) ─
# docker images 는 생성일 내림차순 기본 정렬. ID/참조를 함께 뽑아 보호 대상 제외.
select_removable_app_images() {
  local kept=0
  docker images "$APP_IMAGE_REPO" --format '{{.ID}}|{{.Repository}}:{{.Tag}}' \
    | while IFS='|' read -r id ref; do
        # dangling(<none>) 은 image prune 이 처리하므로 스킵
        [[ "$ref" == *":<none>"* ]] && continue
        # prod-latest 포인터 보존
        [[ "$ref" == *":${LATEST_TAG}" ]] && continue
        # 현재 실행 중인 이미지 ID 보존
        if grep -q "$id" <<<"$PROTECTED_IMAGE_IDS"; then
          continue
        fi
        # 최신 N개 롤백용 보존
        if [[ "$kept" -lt "$APP_IMAGE_KEEP_COUNT" ]]; then
          kept=$((kept + 1))
          continue
        fi
        echo "$ref"
      done
}

REMOVABLE_IMAGES="$(select_removable_app_images || true)"
REMOVABLE_COUNT=0
if [[ -n "$REMOVABLE_IMAGES" ]]; then
  REMOVABLE_COUNT="$(wc -l <<<"$REMOVABLE_IMAGES")"
fi

if [[ "$REMOVABLE_COUNT" -eq 0 ]]; then
  log "INFO" "삭제 대상 오래된 앱 이미지 없음 (보존: 실행중 + ${LATEST_TAG} + 최신 ${APP_IMAGE_KEEP_COUNT}개)"
else
  log "INFO" "삭제 대상 오래된 앱 이미지 ${REMOVABLE_COUNT}개: $(echo "$REMOVABLE_IMAGES" | tr '\n' ' ')"
  while IFS= read -r ref; do
    [[ -z "$ref" ]] && continue
    if [[ "$DRY_RUN" == "1" ]]; then
      log "INFO" "[DRY-RUN] 삭제 예정 이미지: ${ref}"
    else
      if docker rmi "$ref" >/dev/null 2>&1; then
        log "INFO" "이미지 삭제 완료: ${ref}"
      else
        log "WARN" "이미지 삭제 실패(사용 중이거나 이미 없음, 스킵): ${ref}"
      fi
    fi
  done <<<"$REMOVABLE_IMAGES"
fi

# ─── dangling 이미지 / 빌드캐시 / 중지 컨테이너 정리 ──────────────
run_prune() {
  local label="$1"; shift
  if [[ "$DRY_RUN" == "1" ]]; then
    log "INFO" "[DRY-RUN] 실행 예정: ${label} ($*)"
    return 0
  fi
  if "$@" >/dev/null 2>&1; then
    log "INFO" "${label} 완료"
  else
    log "WARN" "${label} 실패(스킵)"
  fi
}

run_prune "dangling 이미지 정리" docker image prune -f
run_prune "빌드 캐시 정리" docker builder prune -f
run_prune "중지 컨테이너 정리" docker container prune -f

# ─── 정리 후 현황 및 확보 용량 ──────────────────────────────────
USED_AFTER="$(disk_used_percent)"
AVAIL_AFTER_MB="$(disk_avail_mb)"
FREED_MB=$((AVAIL_AFTER_MB - AVAIL_BEFORE_MB))
if [[ "$DRY_RUN" == "1" ]]; then
  log "INFO" "DRY-RUN 완료: 실제 삭제 없음 (before 사용률=${USED_BEFORE}%, 삭제예정 이미지=${REMOVABLE_COUNT}개)"
else
  log "INFO" "정리 완료: 사용률 ${USED_BEFORE}%→${USED_AFTER}%, 확보=${FREED_MB}MB, 삭제 이미지=${REMOVABLE_COUNT}개"
fi