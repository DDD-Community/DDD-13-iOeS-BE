#!/usr/bin/env bash
# Usage: bash /path/to/deploy/maintenance/scripts/setup-cron.sh
# 기존 Cron을 보존하면서 디스크 정리 Cron만 추가 (덮어쓰기 금지)

set -euo pipefail

MAINT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# 매일 새벽 4시 (백업 02:00/03:00 과 시간대 분리)
# bash 로 호출: git reset --hard 배포(core.fileMode=false)로 실행 비트가 벗겨져도 동작 보장
CLEANUP_CRON="0 4 * * * bash ${MAINT_DIR}/scripts/cleanup-disk.sh >> ${MAINT_DIR}/logs/cleanup-disk.log 2>&1"

add_cron_if_absent() {
  local entry="$1"
  local current
  current=$(crontab -l 2>/dev/null || true)
  if echo "$current" | grep -qF "$entry"; then
    echo "[SKIP] 이미 등록됨: $entry"
    return
  fi
  (echo "$current"; echo "$entry") | crontab -
  echo "[OK]   등록 완료: $entry"
}

# 로그 디렉토리 생성 및 스크립트 실행 권한 부여
mkdir -p "${MAINT_DIR}/logs"
chmod +x "${MAINT_DIR}/scripts/cleanup-disk.sh"

echo "=== 디스크 정리 Cron 등록 시작 ==="
add_cron_if_absent "$CLEANUP_CRON"

echo ""
echo "=== 현재 Cron 목록 ==="
crontab -l