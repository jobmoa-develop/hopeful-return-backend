-- V32: 내부/외부 직원 구분 플래그 (계정 단위)
-- true(1)=내부 직원(관리자 권한이 없어도 전체 근무자 일정을 읽기전용으로 조회 가능),
-- false(0)=외부 인력(강사 등, 전체 일정 조회 불가).
-- 기존 계정은 DEFAULT 1(내부)로 백필하고, 'LECTURER 역할만' 가진 계정만 외부(0)로 초기화한다.

ALTER TABLE users
    ADD is_internal BIT NOT NULL
    CONSTRAINT DF_USERS_IS_INTERNAL DEFAULT 1;
GO

-- (ALTER 로 추가한 컬럼은 같은 배치에서 참조 불가 → 위 GO 로 배치 분리)
-- LECTURER 역할을 가지면서 다른 역할이 전혀 없는 계정만 외부(0)로 설정.
UPDATE u
    SET u.is_internal = 0
    FROM users u
    WHERE EXISTS (
        SELECT 1
        FROM user_role ur
        JOIN role r ON r.role_id = ur.role_id
        WHERE ur.user_id = u.user_id
          AND r.role_name = 'LECTURER')
    AND NOT EXISTS (
        SELECT 1
        FROM user_role ur2
        JOIN role r2 ON r2.role_id = ur2.role_id
        WHERE ur2.user_id = u.user_id
          AND r2.role_name <> 'LECTURER');
