-- V19: 강좌 담당자(staff) 안내 문자 발송 이력 테이블
-- 강좌 상태변경(STATUS_CHANGE)·일정/장소변경(SCHEDULE_CHANGE) 시 담당자(PM 제외)에게 보낸
-- 안내 문자 발송 이력을 기록한다. participant_sms 는 course_participant_id NOT NULL 로
-- '수강생' 전용 구조라 재사용할 수 없어 별도 테이블로 신설한다.

CREATE TABLE course_staff_sms (
    course_staff_sms_id BIGINT IDENTITY(1,1) NOT NULL,   -- PK
    course_id           BIGINT NOT NULL,                  -- 대상 강좌 → course
    user_id             BIGINT NOT NULL,                  -- 수신 담당자 → users (PM 제외)
    sent_by             BIGINT NULL,                      -- 발송을 트리거한 계정(상태변경/수정한 사람). 시스템 자동이면 NULL 허용
    notify_type         NVARCHAR(30) NOT NULL,             -- STATUS_CHANGE / SCHEDULE_CHANGE
    content             NVARCHAR(2000) NULL,               -- 실제 발송 내용
    send_status         NVARCHAR(50) NULL,                 -- SUCCESS / FAIL
    sent_at             DATETIME2 NULL,
    created_at          DATETIME2 NULL,
    CONSTRAINT PK_COURSE_STAFF_SMS PRIMARY KEY (course_staff_sms_id),
    CONSTRAINT CK_COURSE_STAFF_SMS_NOTIFY_TYPE
        CHECK (notify_type IN ('STATUS_CHANGE', 'SCHEDULE_CHANGE'))
);

ALTER TABLE course_staff_sms
    ADD CONSTRAINT FK_COURSE_STAFF_SMS_COURSE
    FOREIGN KEY (course_id) REFERENCES course (course_id);

ALTER TABLE course_staff_sms
    ADD CONSTRAINT FK_COURSE_STAFF_SMS_USERS
    FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE course_staff_sms
    ADD CONSTRAINT FK_COURSE_STAFF_SMS_SENT_BY_USERS
    FOREIGN KEY (sent_by) REFERENCES users (user_id);

-- 강좌별 발송이력 조회(예: /api/courses/{courseId}/staff-sms-history)에서 사용
CREATE INDEX IX_COURSE_STAFF_SMS_COURSE ON course_staff_sms (course_id);