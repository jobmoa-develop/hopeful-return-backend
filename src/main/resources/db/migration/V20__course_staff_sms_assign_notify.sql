-- V20: course_staff_sms.notify_type 에 인력배정 시나리오 3종 추가
-- 기존 CHECK 제약(STATUS_CHANGE/SCHEDULE_CHANGE)에
-- ASSIGN_NEW / ASSIGN_CHANGED / ASSIGN_REMOVED 를 더한다.
-- MSSQL 은 CHECK 제약을 직접 ALTER 할 수 없어 DROP 후 재생성한다.
-- (컬럼은 NVARCHAR(30) 이라 변경 불필요, enum은 STRING 저장. 기존 값은 반드시 유지.)

ALTER TABLE course_staff_sms
    DROP CONSTRAINT CK_COURSE_STAFF_SMS_NOTIFY_TYPE;

ALTER TABLE course_staff_sms
    ADD CONSTRAINT CK_COURSE_STAFF_SMS_NOTIFY_TYPE
    CHECK (notify_type IN (
        'STATUS_CHANGE',
        'SCHEDULE_CHANGE',
        'ASSIGN_NEW',
        'ASSIGN_CHANGED',
        'ASSIGN_REMOVED'
    ));
