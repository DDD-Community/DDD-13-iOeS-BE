#!/usr/bin/env bash
# Usage: bash /home/ec2-user/backup/scripts/sync-logs.sh
# 로그 증분 백업: 롤링 아카이브(logs/archive/*.gz)를 S3로 sync
# 롤링된 .gz 는 불변이므로 각 파일이 정확히 한 번만 업로드된다.
# 매일 실행되어 logback 의 maxHistory(30일)/totalSizeCap(3GB) 삭제 전에 이관 → 유실 방지.
# Cron: 매일 새벽 3시 30분 (setup-cron.sh 로 등록)

set -euo pipefail

ENV_FILE="$(dirname "$0")/../.env"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "{\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"level\":\"ERROR\",\"type\":\"applog\",\"message\":\".env 파일 없음: ${ENV_FILE}\"}"
  exit 1
fi
# shellcheck source=../.env
source "$ENV_FILE"

ARCHIVE_DIR="${LOG_DIR%/}/archive"

log() {
  local level="$1"
  local message="$2"
  echo "{\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"level\":\"${level}\",\"type\":\"applog\",\"message\":\"${message}\"}"
}

log "INFO" "로그 증분 sync 시작 (dir=${ARCHIVE_DIR})"

# 첫 롤링 전이라 아카이브 디렉토리가 아직 없으면 정상 종료 (유실 아님)
if [[ ! -d "$ARCHIVE_DIR" ]]; then
  log "INFO" "아카이브 없음(스킵): ${ARCHIVE_DIR}"
  exit 0
fi

# S3 sync (IAM Role 인증 - 액세스키 불필요)
# --delete 미사용: S3 측 원본은 절대 삭제하지 않는다.
# --storage-class 미지정: STANDARD 유지.
if ! SYNC_OUT=$(aws s3 sync "$ARCHIVE_DIR" "s3://${S3_BUCKET}/logs/archive/" --region "$S3_REGION" --no-progress 2>&1); then
  log "ERROR" "S3 sync 실패: s3://${S3_BUCKET}/logs/archive/ (${SYNC_OUT//\"/\'})"
  exit 1
fi

UPLOADED=$(echo "$SYNC_OUT" | grep -c '^upload:' || true)
log "INFO" "S3 sync 완료: s3://${S3_BUCKET}/logs/archive/ (업로드 ${UPLOADED}건)"
log "INFO" "로그 증분 sync 성공"
