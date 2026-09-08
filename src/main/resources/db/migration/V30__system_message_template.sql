-- V30: 시스템(하드코딩) 문자 템플릿 DB 이관
-- 여러 페이지·코드에 상수로 흩어져 있던 발송 문구를 목적별 고정 key 단일 행으로 보관한다.
-- 관리자(ADMIN/HEAD_OFFICE)만 본문(content)을 수정할 수 있고, 발송 코드가 이 표를 읽어 실제
-- 발송 문구를 결정한다(수정 즉시 반영). key 는 코드 상수(ASSIGN_NEW 등)와 1:1 대응하며,
-- 신규 key 는 코드 배포와 함께 시드로 추가한다. create/delete 는 API 로 제공하지 않는다.
--   template_key : 코드가 참조하는 고정 식별자(유니크)
--   name         : 관리 화면 표시명(한글 라벨)
--   description  : 용도·치환 토큰 안내(관리 화면 힌트)
--   content      : 실제 발송 본문(치환 토큰 포함, 최대 2000)
-- 줄바꿈은 NCHAR(10)(LF) 로만 삽입한다 — CRLF 는 SENS 바이트 계산/FE 미리보기와 어긋난다.
CREATE TABLE system_message_template (
    system_message_template_id BIGINT IDENTITY(1,1) NOT NULL,
    template_key   NVARCHAR(50)   NOT NULL,
    name           NVARCHAR(100)  NOT NULL,
    description    NVARCHAR(300)  NULL,
    content        NVARCHAR(2000) NOT NULL,
    updated_at     DATETIME2      NULL,
    updated_by     BIGINT         NULL,
    CONSTRAINT PK_SYSTEM_MESSAGE_TEMPLATE PRIMARY KEY (system_message_template_id),
    CONSTRAINT UQ_SYSTEM_MESSAGE_TEMPLATE_KEY UNIQUE (template_key)
);
GO

ALTER TABLE system_message_template
    ADD CONSTRAINT FK_SYSTEM_MESSAGE_TEMPLATE_UPDATED_BY
    FOREIGN KEY (updated_by) REFERENCES users (user_id);
GO

-- 시드: 현재 하드코딩 문구를 그대로 이관(줄바꿈 = NCHAR(10))
INSERT INTO system_message_template (template_key, name, description, content) VALUES
('ASSIGN_NEW', N'인력 배정 안내(최초 배정)',
 N'토큰: {region} 지역, {round} 회차, {startDate} 개강일(M/d), {role} 배정 역할',
 N'[잡모아]' + NCHAR(10) + N'{region} {round}회차({startDate}~) {role}으로 배정' + NCHAR(10) + N'전산에서 확인 부탁드립니다');
GO
INSERT INTO system_message_template (template_key, name, description, content) VALUES
('ASSIGN_CHANGED', N'인력 배정 안내(인력 변동)',
 N'토큰: {region} 지역, {round} 회차, {startDate} 개강일(M/d)',
 N'[잡모아]' + NCHAR(10) + N'{region} {round}회차({startDate}~) 인력변동' + NCHAR(10) + N'전산에서 확인 부탁드립니다');
GO
INSERT INTO system_message_template (template_key, name, description, content) VALUES
('ASSIGN_REMOVED', N'인력 배정 안내(인력 제외)',
 N'토큰: {region} 지역, {round} 회차, {startDate} 개강일(M/d)',
 N'[잡모아]' + NCHAR(10) + N'{region} {round}회차({startDate}~) 인력 제외' + NCHAR(10) + N'전산에서 확인 부탁드립니다');
GO
INSERT INTO system_message_template (template_key, name, description, content) VALUES
('COURSE_OPEN_REMINDER', N'개강 하루전 안내',
 N'토큰: {titleBefore} 하루전/N일전, {region} 지역, {localCourseNumber} 지역회차, {courseNumber} 전체회차, {beforeText} 하루/N일, {staffName} 배정인력명, {schedule} 일정, {location} 교육장 주소',
 N'개강 회차 {titleBefore} 안내' + NCHAR(10) +
 N'{region}({localCourseNumber})_{courseNumber} 개강 {beforeText} 전입니다.' + NCHAR(10) +
 N'아래 방문일자에 지장이 없도록 방문 부탁드립니다.' + NCHAR(10) +
 N'{staffName}' + NCHAR(10) +
 N'일정: {schedule}' + NCHAR(10) +
 N'주소: {location}');
GO
INSERT INTO system_message_template (template_key, name, description, content) VALUES
('VERIFICATION_CODE', N'인증코드 안내',
 N'토큰: {code} 인증코드',
 N'[인증코드] {code}');
GO
