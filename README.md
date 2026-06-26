# hopeful-return-backend

신규 서비스 백엔드 (Spring Boot). **현재는 공유용 기본 구조(인프라 스켈레톤)** 단계 — 도메인/DB 스키마는 미확정.

> 레포 상태: **public** · `main`/`develop` 브랜치 보호 적용(PR + 1인 리뷰 승인). 전체 구조 개요: [docs/프로젝트_구조.md](docs/프로젝트_구조.md)

## 스택
- Java 17, Spring Boot 3.5.5 (Gradle)
- Spring Security + JWT(유틸/필터), Spring Data JPA, Flyway(MSSQL)
- MSSQL, Redis, Mail(SMTP) / SMS(인터페이스, 추후 구현)

## 빠른 시작
```bash
cp .env.example .env            # 값 채우기 (DB_PASSWORD 등)
docker compose up -d            # MSSQL(호스트 14330) + Redis(16379)
export DB_PASSWORD=$(grep '^DB_PASSWORD=' .env | cut -d= -f2)
./gradlew bootRun --args='--spring.profiles.active=local'
curl http://localhost:3434/api/ping   # {"success":true,"data":"pong"}
```
> 가장 간단히 전체를 컨테이너로: `docker compose --profile app up --build`
자세한 내용은 [docs/DOCKER.md](docs/DOCKER.md), 브랜치 규칙은 [docs/BRANCHING.md](docs/BRANCHING.md).

## 현재 구조 (인프라 스켈레톤)
```
com.jobmoa.hopefulreturn
├── config/    SecurityConfig (stateless, CORS, JWT 필터 등록)
├── security/  JwtTokenProvider, JwtAuthenticationFilter (클레임 기반, DB 비의존)
├── common/    ApiResponse(공통 응답), ErrorCode, 전역 예외 처리
├── email/     EmailService(SMTP) + EmailVerificationService(Redis)
├── sms/       SmsService 인터페이스 + NoOp 구현(추후 교체)
└── web/       PingController (/api/ping)
```

## 없음(의도적 미구현 — 확정 후 추가)
- 회원/도메인 엔티티, 로그인/회원가입 API, DB 스키마(Flyway 마이그레이션)

## 다음 단계 (확정 후)
- DB 스키마 → `src/main/resources/db/migration/V1__init.sql` 추가 (Flyway)
- 회원/인증 도메인 → 위 인프라(JWT/SecurityConfig/공통) 위에 구현
