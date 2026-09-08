-- V28: course_staff_sms.notify_type CHECK 제약에 COURSE_OPEN_REMINDER 추가
-- 개강 하루전 자동 문자(스케줄러)는 course_staff_sms 에 notify_type=COURSE_OPEN_REMINDER 로 이력을 남긴다.
-- 그런데 기존 CHECK 제약(V19 신설, V20 확장)이 이 값을 허용하지 않아 실제 발송 시 INSERT 가 실패했다.
-- MSSQL 은 CHECK 제약을 직접 ALTER 할 수 없어 DROP 후 재생성한다. 기존 값은 반드시 유지.
ALTER TABLE course_staff_sms
    DROP CONSTRAINT CK_COURSE_STAFF_SMS_NOTIFY_TYPE;

ALTER TABLE course_staff_sms
    ADD CONSTRAINT CK_COURSE_STAFF_SMS_NOTIFY_TYPE
    CHECK (notify_type IN (
        'STATUS_CHANGE',
        'SCHEDULE_CHANGE',
        'ASSIGN_NEW',
        'ASSIGN_CHANGED',
        'ASSIGN_REMOVED',
        'COURSE_OPEN_REMINDER'
    ));
