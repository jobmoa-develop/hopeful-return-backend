# hopeful-return-backend

국민취업지원 관련 신규 서비스 백엔드 (Spring Boot).

## 스택
- Java 17, Spring Boot 3.5.5 (Gradle)
- Spring Security + JWT, Spring Data JPA, Flyway(MSSQL)
- MSSQL, Redis
- Mail(SMTP) / SMS(인터페이스, 추후 구현)

## 빠른 시작
```bash
cp .env.example .env            # 값 채우기 (DB_PASSWORD 등)
docker compose up -d            # MSSQL + Redis
./gradlew bootRun --args='--spring.profiles.active=local'
```
자세한 내용은 [docs/DOCKER.md](docs/DOCKER.md), 브랜치 규칙은 [docs/BRANCHING.md](docs/BRANCHING.md) 참고.

## 구조
```
com.jobmoa.hopefulreturn
├── config/    SecurityConfig 등
├── security/  JWT 토큰/필터/UserDetails
├── common/    공통 응답·예외
├── member/    회원 도메인
├── auth/      로그인·회원가입·토큰
├── email/     SMTP + Redis 인증코드
└── sms/       SMS 인터페이스(추후 구현)
```

## API (auth)
| Method | Path | 설명 |
|--------|------|------|
| POST | /api/auth/signup | 회원가입 |
| POST | /api/auth/login | 로그인(JWT 발급) |
| POST | /api/auth/refresh | 토큰 재발급 |
| POST | /api/auth/email/send | 이메일 인증코드 발송 |
| POST | /api/auth/email/verify | 이메일 인증코드 검증 |
