#!/usr/bin/env bash
# Usage: bash /home/ec2-user/backup/scripts/backup-db.sh
# DB 백업: pg_dump → gzip 압축 → S3 업로드
# Cron: 매주 일요일 새벽 2시 (setup-cron.sh 로 등록)

set -euo pipefail

ENV_FILE="$(dirname "$0")/../.env"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "{\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"level\":\"ERROR\",\"type\":\"db\",\"message\":\".env 파일 없음: ${ENV_FILE}\"}"
  exit 1
fi
# shellcheck source=../.env
source "$ENV_FILE"

DATE_PATH=$(date +%Y/%m/%d)
DATE_COMPACT=$(date +%Y%m%d-%H%M%S)
TMP_FILE="/tmp/backup-db-${DATE_COMPACT}.sql.gz"

log() {
  local level="$1"
  local message="$2"
  echo "{\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"level\":\"${level}\",\"type\":\"db\",\"message\":\"${message}\"}"
}

cleanup() {
  rm -f "$TMP_FILE"
}
trap cleanup EXIT

log "INFO" "DB 백업 시작 (container=${DB_CONTAINER_NAME}, db=${DB_NAME})"

# /tmp 여유공간 체크
AVAIL_MB=$(df -m /tmp | awk 'NR==2 {print $4}')
if [[ "$AVAIL_MB" -lt "$REQUIRED_SPACE_MB" ]]; then
  log "ERROR" "/tmp 여유공간 부족: 가용=${AVAIL_MB}MB, 필요=${REQUIRED_SPACE_MB}MB"
  exit 1
fi
log "INFO" "디스크 체크 완료 (가용: ${AVAIL_MB}MB)"

# pg_dump → gzip 압축 (nice -n 19 로 CPU 우선순위 낮춤)
if ! nice -n 19 docker exec "$DB_CONTAINER_NAME" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$TMP_FILE"; then
  log "ERROR" "pg_dump 실패 (container=${DB_CONTAINER_NAME})"
  exit 1
fi
log "INFO" "pg_dump 완료"
log "INFO" "gzip 압축 완료 (크기: $(du -sh "$TMP_FILE" | cut -f1))"

# S3 업로드 (IAM Role 인증 - 액세스키 불필요)
S3_KEY="db/${DATE_PATH}/backup-${DATE_COMPACT}.sql.gz"
if ! aws s3 cp "$TMP_FILE" "s3://${S3_BUCKET}/${S3_KEY}" --region "$S3_REGION"; then
  log "ERROR" "S3 업로드 실패: s3://${S3_BUCKET}/${S3_KEY}"
  exit 1
fi
log "INFO" "S3 업로드 완료: s3://${S3_BUCKET}/${S3_KEY}"
log "INFO" "DB 백업 성공"
