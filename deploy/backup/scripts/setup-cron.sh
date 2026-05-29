#!/usr/bin/env bash
# Usage: bash /home/ec2-user/backup/scripts/setup-cron.sh
# 기존 Cron을 보존하면서 백업 Cron만 추가 (덮어쓰기 금지)

set -euo pipefail

BACKUP_DIR="/home/ubuntu/DDD-13-iOeS-BE/deploy/backup"
DB_CRON="0 2 * * 0 ${BACKUP_DIR}/scripts/backup-db.sh >> ${BACKUP_DIR}/logs/backup-db.log 2>&1"
LOGS_CRON="0 3 1 * * ${BACKUP_DIR}/scripts/backup-logs.sh >> ${BACKUP_DIR}/logs/backup-logs.log 2>&1"

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
mkdir -p "${BACKUP_DIR}/logs"
chmod +x "${BACKUP_DIR}/scripts/backup-db.sh"
chmod +x "${BACKUP_DIR}/scripts/backup-logs.sh"

echo "=== 백업 Cron 등록 시작 ==="
add_cron_if_absent "$DB_CRON"
add_cron_if_absent "$LOGS_CRON"

echo ""
echo "=== 현재 Cron 목록 ==="
crontab -l
