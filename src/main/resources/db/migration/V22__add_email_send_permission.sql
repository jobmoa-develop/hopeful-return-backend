-- V22: 메일 발송(근무불가 알림) 수신 권한 플래그 (계정 단위)
-- 근무불가 알림 메일의 수신 대상을 역할이 아닌 계정 단위 권한으로 제어한다.
-- (문자 발송 권한 can_send_sms(V14)와 동일한 패턴)

ALTER TABLE users
    ADD can_send_email BIT NOT NULL
    CONSTRAINT DF_USERS_CAN_SEND_EMAIL DEFAULT 0;
GO

-- 시스템 관리자(ADMIN)에게는 기본 부여
-- (ALTER 로 추가한 컬럼은 같은 배치에서 참조 불가 → 위 GO 로 배치 분리)
UPDATE u
    SET u.can_send_email = 1
    FROM users u
    JOIN user_role ur ON ur.user_id = u.user_id
    JOIN role r ON r.role_id = ur.role_id
    WHERE r.role_name = 'ADMIN';
