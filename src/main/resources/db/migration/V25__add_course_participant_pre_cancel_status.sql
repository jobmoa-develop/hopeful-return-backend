-- 회차 폐강(course.status = CANCELED) 전환 시 참여자 진행상태를 폐강(COURSE_CANCELED)으로 바꾸기 직전의
-- 이전 상태를 보관한다. 회차가 다시 활성 상태로 되돌아오면 이 값으로 참여자 진행상태를 복구한다.
ALTER TABLE course_participant ADD pre_cancel_status NVARCHAR(20) NULL;
