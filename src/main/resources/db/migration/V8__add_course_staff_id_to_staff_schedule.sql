-- 회차 날짜별 인력 배정은 staff_schedule 행을 course_staff(회차·역할·직원)에 연결해 표현한다.
-- staff_schedule 에 course_staff_id(nullable FK) 를 추가한다.
--   - course_staff_id IS NOT NULL : 배정 행(연결된 course_staff 의 회차·역할·session·직원이 schedule_date 에 근무)
--   - course_staff_id IS NULL + is_available = 0 : 사용자/캘린더가 등록한 근무 불가일
--   - course_staff_id IS NULL + is_available = 1 : 일반 가용
-- is_available 은 기존 의미(근무 가능 여부)를 유지한다.
ALTER TABLE staff_schedule
ADD course_staff_id BIGINT NULL;
GO

-- course_staff 삭제 시 딸린 배정 일정 행도 함께 제거되도록 CASCADE.
ALTER TABLE staff_schedule
ADD CONSTRAINT FK_STAFF_SCHEDULE_COURSE_STAFF
FOREIGN KEY (course_staff_id) REFERENCES course_staff (course_staff_id)
ON DELETE CASCADE;
GO
