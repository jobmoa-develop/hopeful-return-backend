-- V11: follow_up 재설계 + course 휴게시간 + follow_up_counsel 신설 + SMS 문자 서비스
-- 사후관리 상담 로그를 follow_up_counsel 로 분리하고, follow_up 은
-- 취업/숲체험/국취연계 '상태 스냅샷'으로 재편한다.
-- 추가로 참여자관리 문자 서비스(개인/공용 템플릿·LMS 본문·MMS 다중 이미지)를 위한 스키마를 확장한다.

-- ── 1) follow_up 컬럼 정리 ───────────────────────────────────────────
-- (a) 상담 로그성 컬럼 제거 → follow_up_counsel 로 이동
ALTER TABLE follow_up DROP COLUMN month_no;
ALTER TABLE follow_up DROP COLUMN contact_date;
ALTER TABLE follow_up DROP COLUMN contact_type;
ALTER TABLE follow_up DROP COLUMN remark;

-- (b) 상태 → 날짜로 의미 변경(값 변환 불가하여 DROP 후 ADD)
ALTER TABLE follow_up DROP COLUMN employment_status;   -- 취업상태(NVARCHAR)
ALTER TABLE follow_up DROP COLUMN national_program;    -- 국취참여상태(NVARCHAR)
ALTER TABLE follow_up DROP COLUMN forest_program;      -- 숲체험 여부(BIT)

-- 컬럼 추가 + CHECK 제약을 한 문장으로(신규 컬럼을 같은 ALTER 안에서 참조 → 배치 분리와 무관하게 안전)
ALTER TABLE follow_up ADD
    employment_date         DATE NULL,          -- 취업일
    forest_program_date     DATE NULL,          -- 숲체험 방문(예정)일
    national_program_date   DATE NULL,          -- 국취 연계일
    national_program_branch NVARCHAR(30) NULL,  -- 국취 연계 지점
    CONSTRAINT CK_FOLLOW_UP_NP_BRANCH CHECK (
        national_program_branch IN (
            N'남부', N'서부', N'부천', N'수원', N'인천서부', N'의정부', N'인천남부',
            N'동대문', N'광명', N'안양', N'북부', N'성남', N'천호', N'관악'
        )
    );

-- ── 2) course 휴게시간 ───────────────────────────────────────────────
ALTER TABLE course ADD breaktime TIME NULL;  -- 휴게시간

-- ── 3) follow_up_counsel (사후관리 상담) 신설 ────────────────────────
CREATE TABLE follow_up_counsel (
    follow_up_counsel_id  BIGINT IDENTITY(1,1) NOT NULL,  -- PK(사후관리상담)
    course_participant_id BIGINT NOT NULL,                -- 회차 참여자 → course_participant
    counsel_number        INT NULL,                       -- 상담번호
    counsel_date          DATE NULL,                      -- 상담일
    counsel_status        NVARCHAR(20) NULL,              -- 유선(landline)/문자(text)/오프라인(offline)
    counsel_memo          NVARCHAR(255) NULL,             -- 상담 메모
    created_at            DATETIME2 NULL,                 -- 생성일(레포 컨벤션)
    updated_at            DATETIME2 NULL,                 -- 수정일(레포 컨벤션)
    CONSTRAINT PK_FOLLOW_UP_COUNSEL PRIMARY KEY (follow_up_counsel_id),
    CONSTRAINT CK_FOLLOW_UP_COUNSEL_STATUS
        CHECK (counsel_status IN ('landline', 'text', 'offline'))
);

ALTER TABLE follow_up_counsel
    ADD CONSTRAINT FK_FOLLOW_UP_COUNSEL_COURSE_PARTICIPANT
    FOREIGN KEY (course_participant_id) REFERENCES course_participant (course_participant_id);

-- ── 4) SMS 문자 서비스 (참여자관리 문자 발송) ────────────────────────
-- 개인/공용 문자 템플릿, participant_sms 확장(LMS 본문·발송형식), MMS 다중 이미지.

-- (a) 문자 템플릿: 개인(user_id 소유)·공용(scope) 제목+내용 저장
CREATE TABLE sms_template (
    sms_template_id BIGINT IDENTITY(1,1) NOT NULL,  -- PK
    user_id         BIGINT NULL,                    -- 개인 템플릿 소유 계정(공용=NULL) → users
    scope           NVARCHAR(10) NOT NULL,          -- PERSONAL(개인) / SHARED(공용)
    title           NVARCHAR(100) NULL,             -- 템플릿 제목
    content         NVARCHAR(2000) NULL,            -- 템플릿 내용
    created_at      DATETIME2 NULL,
    updated_at      DATETIME2 NULL,
    CONSTRAINT PK_SMS_TEMPLATE PRIMARY KEY (sms_template_id),
    CONSTRAINT CK_SMS_TEMPLATE_SCOPE CHECK (scope IN ('PERSONAL', 'SHARED'))
);

ALTER TABLE sms_template
    ADD CONSTRAINT FK_SMS_TEMPLATE_USERS
    FOREIGN KEY (user_id) REFERENCES users (user_id);

-- (b) participant_sms 확장: LMS 본문 길이(255→2000) + 발송 형식(SMS/LMS/MMS)
ALTER TABLE participant_sms ALTER COLUMN content NVARCHAR(2000) NULL;

ALTER TABLE participant_sms ADD
    message_format NVARCHAR(10) NULL              -- SMS / LMS / MMS (FE 판별 결과 기록)
    CONSTRAINT CK_PARTICIPANT_SMS_MESSAGE_FORMAT CHECK (message_format IN ('SMS', 'LMS', 'MMS'));

-- (c) MMS 다중 이미지: participant_sms 1건당 이미지 여러 장(외부 스토리지 URL)
CREATE TABLE participant_sms_image (
    participant_sms_image_id BIGINT IDENTITY(1,1) NOT NULL,  -- PK
    sms_id     BIGINT NOT NULL,          -- 대상 문자 → participant_sms
    image_url  NVARCHAR(500) NOT NULL,   -- 이미지 URL(외부 스토리지)
    sort_order INT NULL,                 -- 표시 순서
    created_at DATETIME2 NULL,
    CONSTRAINT PK_PARTICIPANT_SMS_IMAGE PRIMARY KEY (participant_sms_image_id)
);

ALTER TABLE participant_sms_image
    ADD CONSTRAINT FK_PARTICIPANT_SMS_IMAGE_SMS
    FOREIGN KEY (sms_id) REFERENCES participant_sms (sms_id);
