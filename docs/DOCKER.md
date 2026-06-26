# Docker 로컬 개발 환경

로컬 개발 환경(MSSQL + Redis)을 컨테이너로 통일한다. 배포 이미지는 멀티스테이지 `docker/spring/Dockerfile` 로 추후 확장.

## 포트 (호스트)
- MSSQL: **14330** → 컨테이너 1433  (호스트 네이티브 SQL Server(1433)와 충돌 회피)
- Redis: **16379** → 컨테이너 6379
- Backend: 3434

## 사전 준비
1. Docker Desktop 실행
2. `cp .env.example .env` 후 값 확인
   - `DB_PASSWORD` 는 MSSQL SA 정책(8자+대/소문자/숫자/기호 중 3종). 셸 호환을 위해 `#`, `!` 는 피하고 `_` 사용 권장.

## 방법 1) 전체를 컨테이너로 (가장 간단 — 권장)
```bash
docker compose --profile app up --build
# mssql(14330) + redis(16379) + 백엔드(3434) 가 .env 값으로 함께 기동
```

## 방법 2) 인프라만 컨테이너 + 백엔드는 로컬 gradle
```bash
docker compose up -d                      # mssql + mssql-init(DB 생성) + redis
export DB_PASSWORD=$(grep '^DB_PASSWORD=' .env | cut -d= -f2)
./gradlew bootRun --args='--spring.profiles.active=local'
```
- local 프로파일은 `localhost:14330`(MSSQL), `localhost:16379`(Redis) 로 접속 (포트는 yml 기본값).
- `DB_PASSWORD` 는 반드시 환경변수로 주입해야 한다(yml 기본값 없음).

## 동작 확인 (스모크) — 검증 완료
```bash
curl http://localhost:3434/actuator/health    # {"status":"UP"}
curl http://localhost:3434/api/ping           # {"success":true,"data":"pong"}
```

## 메모
- `mssql-init` 컨테이너가 `hopeful_return` DB 가 없으면 생성한다.
- 현재 마이그레이션이 없어 Flyway 는 "No migrations" 로그만 남기고 정상 부팅(엔티티 0개).
- DB 스키마 확정 시 `src/main/resources/db/migration/V1__init.sql` 추가 → Flyway 자동 적용.
- 메일 헬스체크는 SMTP 미설정으로 비활성(`management.health.mail.enabled=false`). SMTP 설정 후 활성화.
