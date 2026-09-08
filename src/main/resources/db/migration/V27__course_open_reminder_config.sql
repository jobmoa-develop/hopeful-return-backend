-- V27: 개강 하루전 자동 문자 발송 설정
-- 개강 확정(모집마감, CLOSED) 회차의 배정 인력(PL·진행자·강사)에게 개강 N일 전 안내 문자를
-- 매일 스케줄러로 자동 발송한다. 발송 일자(며칠 전)·발송 시각을 관리자가 지정할 수 있도록
-- 단일 설정 행으로 보관한다.
--   enabled       : 자동 발송 on/off
--   days_before   : 개강 며칠 전에 발송할지(기본 1 = 하루 전)
--   send_time     : 발송 시각(스케줄러가 이 시각 이후에 하루 1회 실행)
--   last_run_date : 하루 1회 배치 실행 가드(신규 스케줄러만 사용, 다른 기능과 무관)
CREATE TABLE course_open_reminder_config (
    reminder_config_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    enabled       BIT  NOT NULL DEFAULT 1,
    days_before   INT  NOT NULL DEFAULT 1,
    send_time     TIME NOT NULL DEFAULT '09:00:00',
    last_run_date DATE NULL,
    updated_at DATETIME2 NULL,
    updated_by BIGINT NULL
);
GO

-- 단일 설정 행 시드(기본: 활성·하루 전·오전 9시)
INSERT INTO course_open_reminder_config (enabled, days_before, send_time)
    VALUES (1, 1, '09:00:00');
GO
