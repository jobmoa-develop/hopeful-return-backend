-- V12: course.breaktime(TIME) → course.break_minutes(INT, 분 단위) 전환

ALTER TABLE course
ADD break_minutes INT NULL;

EXEC sp_executesql N'
    UPDATE course
    SET break_minutes = DATEDIFF(MINUTE, ''00:00:00'', breaktime)
    WHERE breaktime IS NOT NULL
';

ALTER TABLE course
DROP COLUMN breaktime;