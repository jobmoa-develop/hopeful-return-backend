-- V29: 관리자 임의 번호 문자 발송 이력
-- course_participant 에 묶이지 않는 원시 전화번호(raw phone)로 발송한 이력을 저장한다.
-- 컬럼 구성은 participant_sms(V1+V15+V16) 를 그대로 따르되, course_participant_id 를 제거하고
-- to_phone(정규화된 수신번호) + recipient_label(관리자 입력 라벨, 선택) 을 둔다.
-- send_status 는 NVARCHAR(50)·CHECK 없음 → RESERVED/CANCELED 값 추가에 별도 변경 불필요(participant_sms 와 동일 정책).

CREATE TABLE admin_sms (
    admin_sms_id    BIGINT IDENTITY(1,1) NOT NULL,   -- PK
    sent_by         BIGINT NULL,                     -- 발송자 → users
    to_phone        NVARCHAR(20) NOT NULL,           -- 수신 전화번호(숫자만 정규화 저장)
    recipient_label NVARCHAR(50) NULL,               -- 선택 라벨(관리자 메모, 없으면 NULL)
    title           NVARCHAR(100) NULL,              -- 제목(LMS/MMS)
    content         NVARCHAR(2000) NULL,             -- 본문
    send_status     NVARCHAR(50) NULL,               -- SUCCESS/FAIL/PENDING/RESERVED/CANCELED
    message_format  NVARCHAR(10) NULL                -- SMS/LMS/MMS
        CONSTRAINT CK_ADMIN_SMS_MESSAGE_FORMAT CHECK (message_format IN ('SMS', 'LMS', 'MMS')),
    sent_at         DATETIME2 NULL,                  -- 발송 일시(예약건은 NULL)
    created_at      DATETIME2 NULL,
    -- 발송결과 추적(participant_sms V15 동일)
    request_id      NVARCHAR(50) NULL,
    message_id      NVARCHAR(50) NULL,
    result_code     NVARCHAR(20) NULL,
    result_message  NVARCHAR(200) NULL,
    complete_time   DATETIME2 NULL,
    -- 예약 발송(participant_sms V16 동일)
    reserve_time    DATETIME2 NULL,
    reserve_id      NVARCHAR(50) NULL,
    CONSTRAINT PK_ADMIN_SMS PRIMARY KEY (admin_sms_id)
);

ALTER TABLE admin_sms
    ADD CONSTRAINT FK_ADMIN_SMS_SENT_BY_USERS
    FOREIGN KEY (sent_by) REFERENCES users (user_id);

-- MMS 첨부 이미지(admin_sms 1건당 여러 장). image_url 에는 SENS 파일 ID(참조) 저장.
CREATE TABLE admin_sms_image (
    admin_sms_image_id BIGINT IDENTITY(1,1) NOT NULL,
    admin_sms_id       BIGINT NOT NULL,             -- 대상 문자 → admin_sms
    image_url          NVARCHAR(500) NOT NULL,      -- SENS 파일 ID(참조)
    sort_order         INT NULL,
    created_at         DATETIME2 NULL,
    CONSTRAINT PK_ADMIN_SMS_IMAGE PRIMARY KEY (admin_sms_image_id)
);

ALTER TABLE admin_sms_image
    ADD CONSTRAINT FK_ADMIN_SMS_IMAGE_SMS
    FOREIGN KEY (admin_sms_id) REFERENCES admin_sms (admin_sms_id);
