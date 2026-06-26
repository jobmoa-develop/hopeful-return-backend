# Docker 로컬 개발 환경

로컬 개발 환경(MSSQL + Redis)을 컨테이너로 통일한다. 배포 이미지는 멀티스테이지 `docker/spring/Dockerfile` 로 추후 확장한다.

## 사전 준비
1. Docker Desktop 실행
2. `cp .env.example .env` 후 값 확인 (특히 `DB_PASSWORD` 는 MSSQL SA 암호 정책 충족: 8자+대/소문자+숫자+기호)

## 1) 인프라만 기동 (권장: 앱은 로컬 gradle 로)
```bash
docker compose up -d            # mssql + mssql-init(DB 생성) + redis
docker compose ps               # 상태 확인
# 백엔드는 호스트에서 실행
export DB_PASSWORD=...           # .env 와 동일 값
./gradlew bootRun --args='--spring.profiles.active=local'
```
- 기동 후 Flyway 가 `V1__init` 마이그레이션을 적용한다 (로그 확인).
- Health: `curl http://localhost:3434/actuator/health` → `{"status":"UP"}`

## 2) 백엔드까지 컨테이너로 (환경 완전 통일)
```bash
docker compose --profile app up --build
```
- `backend` 서비스가 `dev` 프로파일로 기동, 포트 3434 노출.

## 동작 확인 (스모크)
```bash
# 회원가입
curl -X POST http://localhost:3434/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"password123","name":"홍길동"}'

# 로그인 (JWT 발급)
curl -X POST http://localhost:3434/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"password123"}'
```

## 메모
- `mssql-init` 컨테이너가 `hopeful_return` DB 가 없으면 생성한다.
- 로컬 SMTP 미설정 시 이메일 발송 API 는 호출 시점에 실패한다 (`.env` 의 MAIL_* 설정 후 사용).
- 배포 포트는 3434 로 통일 (운영 Ubuntu 24.04 / Nginx 연계는 별도 배포 문서 참고).
