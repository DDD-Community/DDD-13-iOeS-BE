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

## dev 테스트 서버

`dev` 브랜치를 운영 EC2의 잔여 리소스에 함께 올려 앱 팀 검증용으로 운영합니다.
컨테이너는 `photo-app-dev` **하나만** 추가하고 postgres/redis/nginx/모니터링은 운영 스택을 공유합니다.
t3.medium(약 3.8GB)에 두 번째 DB·JVM 세트를 올릴 여유가 없기 때문입니다.

| 자원 | 운영 | 테스트 서버 |
|---|---|---|
| 도메인 | `pickflow-api.us` | `dev-api.pickflow-api.us` |
| 컨테이너 | `photo-app` (`mem_limit` 1300m) | `photo-app-dev` (900m) |
| Spring 프로필 | `prod` | `test` |
| DB | `photo` | `photo_dev` (같은 postgres 인스턴스) |
| Redis | 논리 DB `0` | 논리 DB `1` (같은 인스턴스) |
| S3 키 prefix | `prod/` | `test/` |
| 이미지 태그 | `prod-latest` / `prod-<sha>` | `dev-latest` / `dev-<sha>` |
| Prometheus job | `spring-boot-app` | `spring-boot-app-dev` |
| Loki job 라벨 | `photo-app` | `photo-app-dev` |
| 외부 API 수집 | 활성 | **비활성** (`app.spotinfo.collect.enabled=false`) |

> 모니터링 job 이름과 로그 라벨을 분리한 이유는 알림 오탐 방지입니다. 앱 다운 알림이
> `up{job="spring-boot-app"}` 을 instance 필터 없이 평가하고, Loki 알림 5종이 모두
> `{job="photo-app"}` 셀렉터를 쓰기 때문에, 같은 라벨을 쓰면 테스트 서버를 재배포할 때마다
> 운영 장애 알림이 Discord 로 발송됩니다.

### 최초 1회 준비 (서버 수작업)

```bash
cd <레포>/deploy

# 1) 테스트용 DB 생성 (운영과 같은 postgres 인스턴스, 데이터베이스만 분리)
#    작은따옴표를 써서 컨테이너 안에서 $POSTGRES_USER 가 치환되게 한다.
#    (큰따옴표면 호스트 셸이 빈 값으로 치환해 psql -U 가 깨진다)
#    postgis extension 은 Flyway V1 이 CREATE EXTENSION IF NOT EXISTS 로 처리하므로 생략한다.
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -c "CREATE DATABASE photo_dev;"'

# 2) 로그 디렉토리 (컨테이너 photo 사용자 uid=1000)
mkdir -p logs-dev
sudo chown -R 1000:1000 logs-dev

# 3) 테스트 서버 환경변수. 운영 .env 와 별도 파일이다.
#    .env.example(빈 템플릿)보다 운영 .env 를 복사해 다른 값만 바꾸는 편이 누락이 적다.
#    DB/프로필 관련 값은 docker-compose.dev.yml 의 environment 가 덮으므로 그대로 둬도 된다.
cp .env .env.dev
chmod 600 .env.dev
vi .env.dev
```

`.env.dev`에서 반드시 운영과 다르게 둘 값:

- `JWT_SECRET` — **새로 생성**(`openssl rand -base64 64`). 같은 값이면 운영 토큰이 테스트 서버에서 그대로 통합니다
- `SHARE_BASE_URL=https://dev-api.pickflow-api.us` — `.env.example`에 없는 항목이라 **줄을 새로 추가**해야 합니다.
  누락하면 테스트 서버가 만든 공유 링크의 `og:url`이 운영 도메인을 가리킵니다
- `SPRING_PROFILES_ACTIVE=test` — compose 가 어차피 덮어쓰지만 혼동을 줄이기 위해 맞춰 둡니다

`REDIS_PASSWORD`·`POSTGRES_*`·`S3_*`·외부 API 키는 공유 자원에 접속하므로 운영과 **같은 값**을 그대로 둡니다.
S3 는 같은 버킷을 쓰되 `app.storage.env=test` 라 객체 키가 `test/` prefix 로 분리됩니다.

마지막으로 Cloudflare DNS에 A 레코드 `dev-api`(IP = EC2 EIP, 프록시됨 ON)를 추가합니다.
인증서는 운영 Origin Cert가 `*.pickflow-api.us`를 포함하므로 추가 발급이 필요 없습니다.

### 배포

`dev` 브랜치에 push하면 `deploy-dev.yml`이 이미지를 빌드해 배포합니다. 수동으로 할 때는:

```bash
cd <레포>/deploy
COMPOSE="docker compose -f docker-compose.yml -f docker-compose.dev.yml"

$COMPOSE pull app-dev
$COMPOSE up -d app-dev
$COMPOSE logs -f app-dev

# 헬스체크
curl -H 'Host: dev-api.pickflow-api.us' https://localhost/healthz -k
```

운영 배포(`deploy-prod.yml`)는 EC2에서 `git reset --hard`를 수행하므로, 테스트 서버 배포는
git을 건드리지 않습니다. **배포 설정 파일 변경은 `main` 병합 시점에만 서버에 반영됩니다.**

### 주의사항

- **외부 API 수집이 꺼져 있어** 테스트 서버의 스팟 상세에는 날씨·혼잡도가 비어 보입니다.
  운영과 서비스키를 공유해 호출량이 두 배가 되면 일일 트래픽 한도를 넘겨 운영 수집까지 실패하기 때문입니다.
  해당 화면을 검증해야 하면 dev 전용 서비스키를 발급받아 `.env.dev`에 넣고 프로필 설정을 켜세요.
- **카카오 탈퇴(연동 해제)는 테스트 전용 계정으로만** 하세요. 카카오 앱이 하나뿐이라
  `KAKAO_ADMIN_KEY` 기준으로 동작하며, 테스트 서버에서 탈퇴하면 그 계정의 운영 연동도 끊깁니다.
- `photo_dev`는 백업 대상이 아닙니다(`backup-db.sh`는 운영 DB만 처리). 테스트 데이터이므로 의도된 동작입니다.
- 테스트 서버 컨테이너가 없어도 운영 nginx는 정상 기동합니다. dev 가상호스트는 upstream을 변수로 두어
  요청 시점에 해석하므로, 테스트 서버가 죽어 있으면 502만 응답합니다.

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

## 모니터링 스택

Prometheus + Grafana Loki + Grafana 기반 관측가능성 스택이 포함되어 있습니다.

```
[ 인터넷 ]
    │
    │ /grafana/
    ▼
┌────────────────────────────────────────────────────┐
│  EC2 (Docker network: backend)                     │
│                                                    │
│  ┌─ nginx ─┐   ┌─ app ─┐ ←──── Prometheus         │
│  │ :80/443 │──▶│ :8080 │        (15s 간격 스크레이프)│
│  └────┬────┘   └───────┘                           │
│       │ /grafana/    ↑                             │
│       ▼         logs bind mount                    │
│  ┌─ grafana ─┐  ┌─ promtail ─┐                    │
│  │  :3000    │  │ ./logs 읽기 │──▶ loki:3100       │
│  └───────────┘  └────────────┘                    │
│  node-exporter (호스트 시스템 메트릭)                │
└────────────────────────────────────────────────────┘
```

### 접근 방법

- **Grafana**: `https://pickflow-api.us/grafana/` (Grafana 로그인 필요)
  - `.env`의 `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` 로 초기 로그인
- **Prometheus UI / Loki**: 외부 노출 없음. 필요 시 SSH 터널 사용
  ```bash
  ssh -L 9090:localhost:9090 <ec2-user>@<EIP>  # Prometheus
  ssh -L 3100:localhost:3100 <ec2-user>@<EIP>  # Loki
  ```

### 로그 포맷

앱 로그가 JSON 형식으로 출력되어 Loki에서 필드 검색이 가능합니다.

```
# Loki에서 특정 requestId로 요청 추적
{job="photo-app"} | json | requestId="abc-123"

# ERROR 레벨 로그만 조회
{job="photo-app", level="ERROR"} | json

# 특정 userId 요청 조회
{job="photo-app"} | json | userId="42"
```

### 보안

| 엔드포인트 | 외부 접근 | 비고 |
|---|---|---|
| `/api/actuator/prometheus` | ❌ 403 | nginx deny, Prometheus만 내부에서 접근 |
| `/grafana/` | ✅ | Grafana 로그인 필요 |
| Prometheus `:9090` | ❌ | 외부 포트 미노출 |
| Loki `:3100` | ❌ | 외부 포트 미노출 |

### 설정 파일 구조

```
deploy/monitoring/
├── prometheus/prometheus.yml      # 스크레이프 설정
├── loki/loki-config.yml           # Loki 스토리지/보존 설정
├── promtail/promtail-config.yml   # 로그 수집 파이프라인
└── grafana/provisioning/
    └── datasources/datasources.yml # 데이터소스 자동 프로비저닝
```

---

## 다음 단계 (TODO)

- [x] 실서비스 오픈 전 **Flyway 도입** ✅ 완료
- [ ] **이미지 업로드 S3 전환** (현재 로컬 볼륨)
- [ ] **GitHub Actions + GHCR** 기반 CI/CD (현재 EC2 직접 빌드)
- [ ] Postgres 자동 백업 (cron + S3)
- [ ] TLS/도메인 적용
- [x] 모니터링 (Prometheus/Grafana Loki) ✅ 완료
