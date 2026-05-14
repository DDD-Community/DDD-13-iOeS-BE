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
  - `pgdata` (named) → Postgres 데이터
  - `redis_data` (named) → Redis 빈 마운트 (현재 영속화 비활성: AOF off, RDB off)
  - `app_uploads` (named) → 업로드 이미지 (app RW, nginx RO 공유)
  - `./logs` (bind) → 앱 로그 파일. 호스트에서 직접 접근 가능하도록 bind mount

> ⚠️ **로그 디렉토리 권한**: 컨테이너 내부 `photo` 사용자의 uid는 **1000**입니다. 호스트의 `deploy/logs/` 디렉토리가 uid 1000에게 쓰기 권한이 있어야 합니다.
> ```bash
> mkdir -p deploy/logs
> sudo chown -R 1000:1000 deploy/logs
> ```
> 권한이 안 맞으면 컨테이너가 시작은 되지만 로그가 비어 있게 됩니다.

---

## 프로파일 (test / prod)

`SPRING_PROFILES_ACTIVE` 값으로 동작 모드를 결정합니다. `.env`에서 설정.

| 항목 | `test` (테스트 서버, iOS 협업/검증용) | `prod` (운영 서버) |
|---|---|---|
| Swagger UI | 노출 (`/api/swagger-ui.html`) | 차단 |
| 앱 로그 레벨 | DEBUG | INFO |
| `show-sql` | true | false |
| Actuator endpoints | health, info, metrics, env | health 만 |
| 헬스 상세 | always | never |
| DDL | update | update (이후 Flyway 도입 시 validate) |

**테스트 서버 EC2의 `.env`**:

```env
SPRING_PROFILES_ACTIVE=test
```

**운영 서버 EC2의 `.env`**:

```env
SPRING_PROFILES_ACTIVE=prod
```

같은 `docker-compose.yml`/이미지로 두 환경을 운영하며, 차이는 `.env`로만 통제합니다.

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
vi .env   # 실제 값 채워넣기 (SPRING_PROFILES_ACTIVE 도 환경에 맞게)
chmod 600 .env

# 로그 bind mount 디렉토리 권한 (컨테이너 photo 사용자 uid=1000)
mkdir -p logs
sudo chown -R 1000:1000 logs
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

## TLS(HTTPS) 적용 — Cloudflare 방식

도메인: `pickflow-api.us` (Cloudflare Registrar)
구성: **Cloudflare(Edge SSL) → nginx(443, Origin Cert) → app:8080**

### 1) Cloudflare 측 설정 (대시보드)

- **DNS → 레코드**: A 레코드 추가 (이름 `@`, IP = EC2 EIP, 프록시됨 ON)
- **SSL/TLS → 개요**: 모드를 **전체(엄격함)** 으로 설정
- **SSL/TLS → 원본 서버 → 인증서 만들기**:
  - 호스트 이름: `pickflow-api.us, *.pickflow-api.us`
  - 유효 기간: 15년
  - 발급된 **원본 인증서**와 **프라이빗 키**를 로컬에 저장

### 2) EC2에 인증서 업로드

```bash
# 로컬에서 EC2로 전송
scp -i <pem키> origin.pem origin.key \
  <ec2-user>@<EIP>:/tmp/

# EC2에서
ssh -i <pem키> <ec2-user>@<EIP>
cd <레포>/deploy
mkdir -p nginx/certs
sudo mv /tmp/origin.pem nginx/certs/
sudo mv /tmp/origin.key nginx/certs/
sudo chmod 600 nginx/certs/origin.key
```

> `nginx/certs/`는 `.gitignore`에 이미 제외돼 있어 커밋되지 않습니다.

### 3) nginx 재시작

```bash
docker compose up -d nginx
docker compose logs nginx   # 에러 없는지 확인
```

### 4) 검증

```bash
curl https://pickflow-api.us/api/actuator/health
# → {"status":"UP"}
```

### 5) EC2 보안그룹 강화 (선택, 권장)

80/443 인바운드를 **Cloudflare IPv4 대역**(https://www.cloudflare.com/ips-v4)만 허용하도록 제한.
이러면 EIP 직접 우회 접근이 차단됩니다.

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
