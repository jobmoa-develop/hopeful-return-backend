-- V21: 인력 근무불가 전환 메일 알림 발송 이력 테이블
-- 인력이 '본인 배정된 회차의 날짜'를 가능(is_available=true)→불가(false)로 바꾸면
-- 배정 관리자(ADMIN·OPERATOR·REGIONAL_MANAGER)에게 이메일을 보내고, 그 발송 이력을
-- 여기에 기록한다(수신자 1명당 1행). 감사(audit) 및 발송 현황 추적용.

CREATE TABLE staff_unavailable_notice (
    staff_unavailable_notice_id BIGINT IDENTITY(1,1) NOT NULL,  -- PK
    staff_schedule_id           BIGINT NOT NULL,                 -- 전환된 staff_schedule 행
    course_staff_id             BIGINT NULL,                     -- 배정 연결(course_staff). 판정 근거
    staff_user_id               BIGINT NOT NULL,                 -- 불가로 전환한 인력 → users
    recipient_user_id           BIGINT NOT NULL,                 -- 알림 수신 관리자 → users
    recipient_email             NVARCHAR(100) NULL,              -- 발송 시점 수신 이메일(스냅샷)
    schedule_date               DATE NOT NULL,                   -- 불가로 바뀐 날짜
    session_type                NVARCHAR(30) NULL,               -- AM / PM / FULL
    send_status                 NVARCHAR(50) NULL,               -- SUCCESS / FAIL
    sent_at                     DATETIME2 NULL,
    created_at                  DATETIME2 NULL,
    CONSTRAINT PK_STAFF_UNAVAILABLE_NOTICE PRIMARY KEY (staff_unavailable_notice_id),
    CONSTRAINT CK_STAFF_UNAVAILABLE_NOTICE_STATUS
        CHECK (send_status IS NULL OR send_status IN ('SUCCESS', 'FAIL'))
);

ALTER TABLE staff_unavailable_notice
    ADD CONSTRAINT FK_STAFF_UNAVAILABLE_NOTICE_STAFF_USER
    FOREIGN KEY (staff_user_id) REFERENCES users (user_id);

ALTER TABLE staff_unavailable_notice
    ADD CONSTRAINT FK_STAFF_UNAVAILABLE_NOTICE_RECIPIENT_USER
    FOREIGN KEY (recipient_user_id) REFERENCES users (user_id);

-- staff_schedule 행 기준 이력 조회(중복 발송 여부 확인 등)
CREATE INDEX IX_STAFF_UNAVAILABLE_NOTICE_SCHEDULE ON staff_unavailable_notice (staff_schedule_id);
