-- V33: 내 일정 Google Calendar ICS 구독 피드용 비밀 토큰 (계정 단위)
-- 사용자별 추측 불가능한 토큰으로 /api/calendar/feed/{token}.ics 를 식별한다.
-- 토큰 non-null = 연동됨, NULL = 미연동/해제(기존 refresh_token 과 동일한 운영 방식).

-- NVARCHAR: 이 프로젝트는 use_nationalized_character_data=true 라 Hibernate 가 모든 String 을
-- NVARCHAR 로 바인딩한다. 토큰 자체는 ASCII 지만 타입을 기존 text 컬럼(nvarchar)과 맞춰야
-- 조회 시 varchar→NCHAR 변환 오류가 나지 않는다.
ALTER TABLE users ADD calendar_feed_token NVARCHAR(64) NULL;
GO

-- 필터 유니크 인덱스: MSSQL 일반 UNIQUE 인덱스는 NULL 을 동등 취급해
-- 토큰 미보유(NULL) 계정이 2명 이상이면 충돌한다. WHERE ... IS NOT NULL 로 회피.
CREATE UNIQUE INDEX UX_USERS_CALENDAR_FEED_TOKEN
    ON users(calendar_feed_token) WHERE calendar_feed_token IS NOT NULL;
GO
