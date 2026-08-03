-------------------------------------------------------
-- V9: PM(박문순) 계정 + 역할별 테스트 계정
--  - 박문순 대표(loginId=moon0300)를 유일한 PROJECT_MANAGER(PM)로 시드. 추후 추가 가능.
--  - 인력 배정 테스트용 역할별 계정(ADMIN 제외, PM은 박문순으로 대체하여 test_pm* 미생성).
--  - 이름은 식별을 위해 성씨(김·이·박·최·정) + 역할로 부여한다.
--  - 비밀번호: 기존 시드(V4)와 동일 BCrypt 해시(평문 1234) 재사용.
--  - IF NOT EXISTS / NOT EXISTS 가드로 멱등.
-------------------------------------------------------

DECLARE @PASSWORD NVARCHAR(255);
SET @PASSWORD = '$2a$10$xPyXJ8Zu57M0tOKmdDUZEuzVfclTHe6oYBvt2hiAtQrdU2GO2hHUy';

-------------------------------------------------------
-- 1) PM 계정: 박문순 대표 (moon0300)
-------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='moon0300')
INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at)
VALUES ('moon0300',@PASSWORD,N'박문순','01003000300','moon0300@jobmoa.com',1,0,0,GETDATE());

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name='PROJECT_MANAGER'
WHERE u.login_id='moon0300'
AND NOT EXISTS (
    SELECT 1 FROM user_role ur WHERE ur.user_id=u.user_id AND ur.role_id=r.role_id
);

-------------------------------------------------------
-- 2) 역할별 테스트 계정 (ADMIN·PM 제외 7역할 × 5) — 성씨 + 역할
-------------------------------------------------------

-- HEAD_OFFICE (본부장)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_head01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_head01',@PASSWORD,N'김본부장','01010101','test_head01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_head02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_head02',@PASSWORD,N'이본부장','01010102','test_head02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_head03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_head03',@PASSWORD,N'박본부장','01010103','test_head03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_head04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_head04',@PASSWORD,N'최본부장','01010104','test_head04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_head05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_head05',@PASSWORD,N'정본부장','01010105','test_head05@test.jobmoa.com',1,0,0,GETDATE());

-- REGIONAL_MANAGER (지역담당자)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_region01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_region01',@PASSWORD,N'김지역담당','01010201','test_region01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_region02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_region02',@PASSWORD,N'이지역담당','01010202','test_region02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_region03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_region03',@PASSWORD,N'박지역담당','01010203','test_region03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_region04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_region04',@PASSWORD,N'최지역담당','01010204','test_region04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_region05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_region05',@PASSWORD,N'정지역담당','01010205','test_region05@test.jobmoa.com',1,0,0,GETDATE());

-- OPERATOR (행정 → 배정 시 ADMIN_STAFF)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_oper01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_oper01',@PASSWORD,N'김행정','01010301','test_oper01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_oper02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_oper02',@PASSWORD,N'이행정','01010302','test_oper02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_oper03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_oper03',@PASSWORD,N'박행정','01010303','test_oper03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_oper04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_oper04',@PASSWORD,N'최행정','01010304','test_oper04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_oper05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_oper05',@PASSWORD,N'정행정','01010305','test_oper05@test.jobmoa.com',1,0,0,GETDATE());

-- COUNSELOR (상담사)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_counsel01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_counsel01',@PASSWORD,N'김상담','01010401','test_counsel01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_counsel02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_counsel02',@PASSWORD,N'이상담','01010402','test_counsel02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_counsel03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_counsel03',@PASSWORD,N'박상담','01010403','test_counsel03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_counsel04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_counsel04',@PASSWORD,N'최상담','01010404','test_counsel04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_counsel05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_counsel05',@PASSWORD,N'정상담','01010405','test_counsel05@test.jobmoa.com',1,0,0,GETDATE());

-- LECTURER (강사)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_lect01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_lect01',@PASSWORD,N'김강사','01010501','test_lect01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_lect02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_lect02',@PASSWORD,N'이강사','01010502','test_lect02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_lect03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_lect03',@PASSWORD,N'박강사','01010503','test_lect03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_lect04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_lect04',@PASSWORD,N'최강사','01010504','test_lect04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_lect05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_lect05',@PASSWORD,N'정강사','01010505','test_lect05@test.jobmoa.com',1,0,0,GETDATE());

-- STAFF (진행요원)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_staff01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_staff01',@PASSWORD,N'김진행','01010601','test_staff01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_staff02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_staff02',@PASSWORD,N'이진행','01010602','test_staff02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_staff03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_staff03',@PASSWORD,N'박진행','01010603','test_staff03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_staff04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_staff04',@PASSWORD,N'최진행','01010604','test_staff04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_staff05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_staff05',@PASSWORD,N'정진행','01010605','test_staff05@test.jobmoa.com',1,0,0,GETDATE());

-- PROJECT_LEADER (PL)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_pl01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_pl01',@PASSWORD,N'김리더','01010801','test_pl01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_pl02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_pl02',@PASSWORD,N'이리더','01010802','test_pl02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_pl03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_pl03',@PASSWORD,N'박리더','01010803','test_pl03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_pl04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_pl04',@PASSWORD,N'최리더','01010804','test_pl04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_pl05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_pl05',@PASSWORD,N'정리더','01010805','test_pl05@test.jobmoa.com',1,0,0,GETDATE());

-------------------------------------------------------
-- 3) user_role 매핑 (test_ 접두어 → 역할)
-------------------------------------------------------
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r
  ON r.role_name = CASE
       WHEN u.login_id LIKE 'test_head%'    THEN 'HEAD_OFFICE'
       WHEN u.login_id LIKE 'test_region%'  THEN 'REGIONAL_MANAGER'
       WHEN u.login_id LIKE 'test_oper%'    THEN 'OPERATOR'
       WHEN u.login_id LIKE 'test_counsel%' THEN 'COUNSELOR'
       WHEN u.login_id LIKE 'test_lect%'    THEN 'LECTURER'
       WHEN u.login_id LIKE 'test_staff%'   THEN 'STAFF'
       WHEN u.login_id LIKE 'test_pl%'      THEN 'PROJECT_LEADER'
     END
WHERE u.login_id LIKE 'test\_%' ESCAPE '\'
AND NOT EXISTS (
    SELECT 1 FROM user_role ur WHERE ur.user_id=u.user_id AND ur.role_id=r.role_id
);
