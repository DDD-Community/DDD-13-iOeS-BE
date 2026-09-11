<h1 align="center">pickflow</h1>
<p align="center"><b>사진 출사 스팟 큐레이션 서비스 - Backend API</b></p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-orange?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-brightgreen?logo=springboot&logoColor=white">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-PostGIS-4169E1?logo=postgresql&logoColor=white">
  <img alt="Redis" src="https://img.shields.io/badge/Redis-cache-DC382D?logo=redis&logoColor=white">
</p>

---

## 소개

**pickflow**는 노을/윤슬/햇살/야경 등 테마별 사진 출사 스팟을 지도 기반으로 탐색하고, 직접 등록/공유할 수 있는 사진 출사 큐레이션 서비스입니다. 이 저장소는 해당 서비스의 백엔드 API 서버입니다.

> 운영 서비스의 영향을 줄 수 있는 정보는 포함하지 않습니다.

## 주요 기능

- **인증**: 소셜로그인 및 JWT토큰 로그아웃 시 블랙리스트 기능
- **스팟 탐색**: 위치기반 지도/리스트 검색, 거리순/추천순 정렬, 테마 필터 기능
- **스팟 등록/공개 워크플로**: 등록 시 비공개로 시작 -> 사용자가 공개 요청 -> 관리자 검수(승인/반려) -> 공개 전환
- **북마크 / 좋아요**: 관심 스팟 저장 및 좋아요 기능
- **이미지 처리**: 업로드 이미지 관리 및 썸네일/CDN 환경 구성, 이미지 접근권한(공개/비공개)에 따른 URL 발급 정책 분리
- **신고 / 공지사항**: 잘못된 정보 신고 공지사항(게시판) 기능
- **어드민**: 스팟 검수, 등록 편의 기능 등 관리자 전용 API
- **외부 API연동** 날씨데이터, 혼잡도 등 외부 API를 통한 스팟 추가 데이터 제공
- **나만의 스팟 기록**: 다른 스팟을 북마크하여 나만의 스팟 저장소에 기록할 수 있는 기능

## 기술 스택

| 영역 | 기술 |
|---|---|
| Language / Framework | Java, Spring Boot, Spring Security, Spring Validation |
| Data Access | Spring Data JPA, MyBatis, Flyway |
| Database | PostgreSQL + PostGIS, Redis |
| Storage | AWS S3 / CloudFront (로컬 개발은 MinIO로 대체) |
| 인증 | JWT, OAuth2 (Kakao, Apple) |
| 문서화 | springdoc-openapi (Swagger UI) |
| 테스트 | JUnit5, Mockito, AssertJ |
| 인프라 | Docker / Docker Compose, Nginx |
| 모니터링 | Prometheus, Grafana, Loki |
| CI | GitHub Actions, AI 기반 자동 코드 리뷰 |

## 아키텍처

```
src/main/java/com/ioes/photo
├── domain      # 도메인 로직 (entity / controller / dto / service / repository)
│   ├── user
│   ├── spot
│   ├── myspot
│   ├── savedspot
│   ├── spotlike
│   ├── spotinfo
│   └── ...
├── external    # 외부 API 연동 (날씨, 혼잡도 등)
└── global      # 공통 모듈 (auth, config, error, logging, storage)
```

- 도메인별로 entity/controller/dto/service/repository를 일관되게 구성
- 위치 기반 검색은 PostGIS 공간 함수를 활용하며, JPA가 지원하지 않는 공간 인덱스/동적 쿼리는 MyBatis로 분리 처리
- 모든 API 응답은 `ApiResponse<T>` 포맷으로 통일
- 도메인 예외는 `BusinessException` + `ErrorCode`로, 그 외(인증/외부 API 등)는 전역 예외 핸들러로 처리

## 실행 환경 (프로필)

| 프로필 | 용도 | 이미지 저장소 |
|---|---|---|
| `dev` | 로컬 개발 | MinIO |
| `test` | 테스트 서버 | AWS S3 + CloudFront |
| `prod` | 운영 서버 | AWS S3 + CloudFront |

## 로컬 개발 환경 실행

### 요구사항
- Java 21
- Docker (MinIO, PostgreSQL+PostGIS, Redis)

### 1. 로컬 이미지 저장소 기동

```bash
docker compose -f docker/minio/docker-compose.yml up -d
```

### 2. 환경변수 설정

보안상 민감한 정보라 문서에는 별도로 표기하지 

### 3. 서버 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 4. API 문서 확인

```
http://localhost:8080/api/swagger-ui.html
```

## 테스트

```bash
./gradlew test
```

- 단위테스트: JUnit5 + Mockito + AssertJ
- 일부 도메인은 Testcontainers 기반 실제 PostgreSQL/Redis 환경에서 통합테스트 수행

## 코드 컨벤션

- DTO는 Java Record + Jakarta Validation, `Request`/`Response` 접미사 규칙
- 모든 Enum은 `CodedEnum` 구현 + 전용 Converter로 DB 코드 매핑
- 모든 Entity는 `BaseEntity`(id/createdAt/updatedAt) 상속, Setter 대신 Builder/명시적 메소드 사용
- Controller는 로직을 포함하지 않고 Service로 위임, 조회는 GET + `@RequestParam`


## CI

- GitHub Actions 기반 빌드/테스트 파이프라인
- AI 기반 자동 코드 리뷰로 PR 품질 1차 검증
 
## AI 활용

- 클로드코드를 기반으로 개발 및 문서화 진행
- 하네스 및 agent화 하여 AI를 활용한 생산성 및 품질 보장

## 비고사항
운영 서비스의 영향을 주지 않는 선에서 해당 저장소에 공개된 정보를 요약하여 정리했습니다

기능이 추가되거나, 누락/오류가 있는 부분은 추후 문서에 업데이트될 예정입니다.