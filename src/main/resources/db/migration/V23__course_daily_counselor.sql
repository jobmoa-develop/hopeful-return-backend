-- 상담사(COUNSELOR) 일별 배정을 staff_schedule UNIQUE(user, date, session) 제약에서 분리한다.
-- 상담사는 여러 지역을 순회 상담하므로 '같은 날 여러 회차'에 중복 배정될 수 있어야 하는데,
-- staff_schedule 은 (user_id, schedule_date, session_type) 가 UNIQUE 라 한 날짜에 1건만 배정 가능했다.
--   - course_staff 의 상담사 로스터 행(역할·회차·인력)은 그대로 유지한다.
--   - 일자 정보만 이 신규 테이블(course_staff_id 링크)에 옮겨 회차별 중복 배정을 허용한다.
--   - 상담사 근무 불가일(staff_schedule.course_staff_id NULL + is_available=0)은 staff_schedule 에 그대로 둔다.
CREATE TABLE course_daily_counselor (
    course_daily_counselor_id BIGINT IDENTITY(1,1) NOT NULL,  -- PK
    course_staff_id           BIGINT NOT NULL,                 -- 상담사 로스터(course_staff, role=COUNSELOR) 링크
    schedule_date             DATE NOT NULL,                   -- 배정 날짜(교육일)
    created_at                DATETIME2 NULL,
    CONSTRAINT PK_COURSE_DAILY_COUNSELOR PRIMARY KEY (course_daily_counselor_id),
    -- 같은 회차·상담사가 같은 날 중복 배정되는 것만 막는다. 회차가 다르면 course_staff 가 달라 중복 허용.
    CONSTRAINT UQ_COURSE_DAILY_COUNSELOR UNIQUE (course_staff_id, schedule_date)
);
GO

-- course_staff 삭제(상담사 로스터 제거) 시 딸린 일별 배정 행도 함께 정리.
ALTER TABLE course_daily_counselor
    ADD CONSTRAINT FK_COURSE_DAILY_COUNSELOR_COURSE_STAFF
    FOREIGN KEY (course_staff_id) REFERENCES course_staff (course_staff_id)
    ON DELETE CASCADE;
GO

CREATE INDEX IX_COURSE_DAILY_COUNSELOR_DATE ON course_daily_counselor (schedule_date);
GO

-- 기존 상담사 배정(staff_schedule 배정행: course_staff_id NOT NULL, 역할 COUNSELOR)을 신규 테이블로 이관.
-- 과거 데이터가 동일 course_staff·날짜에 AM/PM 2행을 가질 수 있어(구 경로는 세션별 저장) UQ(cs,date)
-- 위반을 피하려 (course_staff_id, schedule_date) 로 중복 제거해 옮긴다.
INSERT INTO course_daily_counselor (course_staff_id, schedule_date, created_at)
SELECT ss.course_staff_id, ss.schedule_date, MIN(ss.created_at)
FROM staff_schedule ss
JOIN course_staff cs ON cs.course_staff_id = ss.course_staff_id
WHERE cs.staff_role = 'COUNSELOR' AND ss.course_staff_id IS NOT NULL
GROUP BY ss.course_staff_id, ss.schedule_date;
GO

-- 이관된 원본 상담사 배정행은 staff_schedule 에서 제거(불가일 행 course_staff_id NULL 은 유지).
DELETE ss
FROM staff_schedule ss
JOIN course_staff cs ON cs.course_staff_id = ss.course_staff_id
WHERE cs.staff_role = 'COUNSELOR' AND ss.course_staff_id IS NOT NULL;
GO
