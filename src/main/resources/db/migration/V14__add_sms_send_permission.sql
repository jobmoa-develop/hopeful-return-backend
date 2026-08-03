-- V14: 문자(SMS) 발송 권한 플래그 (계정 단위)
-- 문자 발송/템플릿 기능은 페이지권한이 아닌 '계정 단위 문자발송 권한'을 부여받은 계정만 사용한다.
-- (페이지권한 방식은 폐기 확정 → 기능별 권한은 users 플래그로 별도 관리)

ALTER TABLE users
    ADD can_send_sms BIT NOT NULL
    CONSTRAINT DF_USERS_CAN_SEND_SMS DEFAULT 0;
GO

-- 시스템 관리자(ADMIN)에게는 기본 부여
-- (ALTER 로 추가한 컬럼은 같은 배치에서 참조 불가 → 위 GO 로 배치 분리)
UPDATE u
    SET u.can_send_sms = 1
    FROM users u
    JOIN user_role ur ON ur.user_id = u.user_id
    JOIN role r ON r.role_id = ur.role_id
    WHERE r.role_name = 'ADMIN';
