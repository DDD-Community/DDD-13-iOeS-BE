# Photo 서비스 배포 가이드

Docker Compose 기반으로 **Nginx + Spring Boot + PostgreSQL + Redis** 스택을 한 번에 기동합니다.

## 구성

```
[ Client ]
    │
    │ 80/443
    ▼
┌────────────────────────────────────────────┐
│  EC2 (Docker network: backend)             │
│                                            │
│  ┌─ nginx ─┐   ┌─ app ─┐   ┌─ postgres ─┐  │
│  │ :80/443 │──▶│ :8080 │──▶│  :5432     │  │
│  └─────────┘   │       │   └────────────┘  │
│       │        │       │   ┌─ redis ────┐  │
│       │        │       │──▶│  :6379     │  │
│       │        └───────┘   └────────────┘  │
│       │                                    │
│       └─ /uploads (read-only 공유 볼륨) ──▶ app_uploads
└────────────────────────────────────────────┘
```

- **외부 노출 포트**: Nginx의 `80/443`만. DB/Redis는 절대 외부 노출 금지.
- **볼륨**:
  - `pgdata` → Postgres 데이터
  - `redis_data` → Redis AOF
  - `app_uploads` → 업로드 이미지 (app가 RW, nginx가 RO 공유)
  - `app_logs` → 앱 로그 파일

---

## 로컬에서 먼저 검증

EC2 올리기 전에 **로컬에서 compose 스택이 뜨는지 확인**하는 게 안전합니다.

```bash
cd deploy
cp .env.example .env

# .env 값 채우기 (최소한 아래 4개는 실제 값으로)
#   POSTGRES_PASSWORD, REDIS_PASSWORD, JWT_SECRET, DATA_GO_KR_SERVICE_KEY, SEOUL_API_KEY

# JWT_SECRET 생성:
openssl rand -base64 64

# 스택 기동
docker compose up -d --build

# 로그 확인
docker compose logs -f app

# 헬스체크
curl http://localhost/api/actuator/health
# → {"status":"UP"}
```

정상 동작 확인되면 스택 종료:

```bash
docker compose down
# 데이터까지 초기화하려면:
docker compose down -v
```

---

## EC2 배포 절차

### 1) EC2 인스턴스 준비

- **사양**: t3.medium (vCPU 2, RAM 4GB) 이상 권장
- **OS**: Amazon Linux 2023 또는 Ubuntu 22.04+
- **스토리지**: 20GB 이상 (업로드 이미지 누적 고려)
- **보안그룹 인바운드**:
  - `22` (SSH) — 내 IP만
  - `80` (HTTP)
  - `443` (HTTPS)
  - `5432`, `6379` **절대 열지 말 것**

### 2) Docker 설치

Amazon Linux 2023:

```bash
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
# 로그아웃 후 재접속
```

Ubuntu 22.04+:

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
# 로그아웃 후 재접속
```

Docker Compose plugin 확인:

```bash
docker compose version
# → Docker Compose version v2.x.x
```

### 3) 스왑 메모리 추가 (t3.medium 이하 권장)

gradle 빌드 시 메모리 부족 방지:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 4) 레포 clone 및 환경변수 설정

```bash
git clone https://github.com/DDD-Community/DDD-13-iOeS-BE.git
cd DDD-13-iOeS-BE/deploy

cp .env.example .env
vi .env   # 실제 값 채워넣기
chmod 600 .env
```

### 5) 스택 기동

```bash
docker compose up -d --build

# 상태 확인
docker compose ps
docker compose logs -f app
```

모든 서비스가 `healthy` 상태가 되면 접속 가능:

```bash
curl http://<EC2_PUBLIC_IP>/api/actuator/health
```

### 6) 운영 명령어

```bash
# 재시작
docker compose restart app

# 코드 업데이트 후 재배포
git pull
docker compose up -d --build app

# 전체 중단
docker compose down

# 로그 tail
docker compose logs -f --tail=100 app
docker compose logs -f nginx

# 앱 컨테이너 진입
docker compose exec app sh

# DB 백업 (예시)
docker compose exec postgres pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" \
  | gzip > backup-$(date +%Y%m%d).sql.gz
```

---

## TLS(HTTPS) 적용 — 도메인 확보 후

1. 도메인 구매 → A 레코드에 EC2 퍼블릭 IP 등록
2. certbot으로 인증서 발급:

   ```bash
   docker run --rm -it \
     -v /etc/letsencrypt:/etc/letsencrypt \
     -v $(pwd)/nginx/certbot:/var/www/certbot \
     -p 80:80 \
     certbot/certbot certonly --standalone -d your-domain.com \
       --agree-tos -m you@example.com --no-eff-email
   ```

3. `deploy/docker-compose.yml`의 nginx 서비스에 인증서 볼륨 마운트 추가:

   ```yaml
   nginx:
     volumes:
       - /etc/letsencrypt:/etc/letsencrypt:ro
       - ./nginx/certbot:/var/www/certbot
   ```

4. `deploy/nginx/conf.d/app.conf`에서 HTTPS 블록 주석 해제, HTTP는 301 리다이렉트로 전환
5. `docker compose up -d nginx`로 반영

---

## 트러블슈팅

| 증상 | 원인 / 조치 |
|---|---|
| `app` 컨테이너가 계속 재시작 | `docker compose logs app`에서 원인 확인. 보통 env 누락 또는 DB 연결 실패 |
| `postgres`는 healthy인데 app이 DB 연결 실패 | `.env`의 `POSTGRES_USER`/`POSTGRES_PASSWORD`와 `PROD_DB_*`가 일치하는지 확인 |
| 로컬에선 되는데 EC2에선 OOM | 스왑 활성화 확인, `JAVA_TOOL_OPTIONS`의 `MaxRAMPercentage` 조정 |
| 업로드한 이미지가 `/uploads/<name>`에서 404 | app 컨테이너가 `/app/uploads`에 쓰고 있는지 확인. `docker compose exec app ls /app/uploads` |
| nginx가 502 Bad Gateway | app healthcheck 실패 상태. `docker compose ps`로 확인 |

---

## 다음 단계 (TODO)

- [ ] 실서비스 오픈 전 **Flyway 도입** (현재 `ddl-auto: update`)
- [ ] **이미지 업로드 S3 전환** (현재 로컬 볼륨)
- [ ] **GitHub Actions + GHCR** 기반 CI/CD (현재 EC2 직접 빌드)
- [ ] Postgres 자동 백업 (cron + S3)
- [ ] TLS/도메인 적용
- [ ] 모니터링 (Prometheus/Grafana or CloudWatch)
