# hopeful-return-backend

희망리턴(취업성공패키지 운영) 서비스 백엔드 (Spring Boot). 인증·참여자관리·출결·상담·문자(SMS)
발송까지 운영에 필요한 도메인 API를 제공한다.

> 레포 상태: **public** · `main`/`develop` 브랜치 보호(PR + 1인 리뷰 승인).
> 스택·패키지 구조는 아래 섹션 참고 · API 문서 색인: [docs/](docs/)

## 스택
- Java 17, Spring Boot 3.5.5 (Gradle)
- Spring Security + JWT(AccessToken + RefreshToken 쿠키), Spring Data JPA, Flyway(MSSQL)
- MSSQL(주 DB), Redis(이메일 인증 등), Mail(SMTP)
- SMS: Naver Cloud **SENS**(비활성 시 NoOp 로거로 대체)
- Excel 일괄등록: Apache POI · API 문서: springdoc OpenAPI(Swagger UI)

## 빠른 시작
```bash
cp .env.example .env            # 값 채우기 (DB_PASSWORD, JWT_SECRET 등)
docker compose up -d            # MSSQL(호스트 14330) + Redis(16379)
export DB_PASSWORD=$(grep '^DB_PASSWORD=' .env | cut -d= -f2)   # bootRun에 DB 비밀번호 전달
./gradlew bootRun --args='--spring.profiles.active=local'
curl http://localhost:3434/api/ping   # {"success":true,"data":"pong"}
```
> 전체를 컨테이너로: `docker compose --profile app up --build`
> 자세한 내용은 [docs/DOCKER.md](docs/DOCKER.md), 브랜치 규칙은 [docs/BRANCHING.md](docs/BRANCHING.md).

## 인증
- `POST /api/auth/login` → AccessToken(JSON 응답) + RefreshToken(**HTTP-only 쿠키**)
- `POST /api/auth/refresh` (쿠키 기반 재발급) · `POST /api/auth/logout` · `GET /api/auth/me`
- `SecurityConfig`(stateless·CORS) + `JwtTokenProvider` + `JwtAuthenticationFilter`
  (클레임에 userId·loginId·roles·SMS 발송 권한 포함, 401/403 핸들러 분리)

## 도메인 · API (약 21개 컨트롤러)
- **인증/사용자/권한**: `/api/auth`, `/api/users`, `/api/roles`, `/api/user-roles`, `/api/regions`
- **강좌/인력**: `/api/courses`, `/api/course-staffs`, `/api/course-daily-staffs`
- **참여자**: `/api/participants`, `/api/course-participants`(Excel 일괄등록·상담사 배정),
  `/api/participant-memos`
- **출결**: `/api/attendances`, `/api/attendance-leaves`
- **사후관리**: `/api/follow-ups`, `/api/follow-up-counsels`
- **문자(SMS)**: `/api/participant-sms`(발송·이력·전달상태 조회), `/api/sms-templates`
- **기타**: `/api/staff-schedules`, `/api/dashboard`, `/allowance`

## 패키지 구조
```
com.jobmoa.hopefulreturn
├── config/         SecurityConfig, SwaggerConfig
├── security/       JwtTokenProvider, JwtAuthenticationFilter, 인증 진입/거부 핸들러
├── common/         ApiResponse(공통 응답), ErrorCode, 전역 예외 처리
├── auth/           로그인·토큰·현재 사용자
├── users, role, userrole, region           사용자·권한·지역
├── course, coursestaff, coursedailystaff    강좌·인력 배정
├── participant, courseparticipant, participantmemo   참여자·수강·메모
├── attendance, attendanceleave              출결·조퇴/외출
├── followup, followupcounsel                사후관리·상담
├── sms, smstemplate, participantsms         SENS 발송·템플릿·발송이력
├── email, dashboard, staffschedule, allowance
└── web/            PingController (/api/ping)
```

## DB (Flyway)
- 마이그레이션: `src/main/resources/db/migration/` — 현재 **V1 ~ V15**
  (V10 상담 슬롯, V11 사후관리 상담·SMS, V14 SMS 발송 권한, V15 SMS 전달결과 등)
- `spring.jpa.hibernate.ddl-auto=validate` — 스키마 변경은 반드시 Flyway 마이그레이션으로 반영

## 프론트엔드 번들링
- `build.gradle`의 `installFrontend`/`buildFrontend`/`copyFrontend`가 `../hopeful_return_front`를
  빌드해 `src/main/resources/static/`에 포함(단일 JAR 배포).
- FE dev 서버와 **동시 구동** 시 파일 락 충돌 방지:
  `./gradlew bootRun -x installFrontend -x buildFrontend --args='--spring.profiles.active=local'`

## 문서
- 백엔드 API·운영 문서: [docs/](docs/) (예: [participant-sms-api.md](docs/participant-sms-api.md),
  [participant-role-scope.md](docs/participant-role-scope.md))
- 공통 문서(ERD·개발일지·운영)는 상위 저장소 루트 `docs/` 참고.
