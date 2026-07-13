-------------------------------------------------------
-- V9: PM(박문순) 계정 + 역할별 테스트 계정
--  - 박문순 대표(loginId=moon0300)를 유일한 PROJECT_MANAGER(PM)로 시드. 추후 추가 가능.
--  - 인력 배정 테스트용 역할별 계정(ADMIN 제외, PM은 박문순으로 대체하여 test_pm* 미생성).
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
-- 2) 역할별 테스트 계정 (ADMIN·PM 제외 7역할 × 5)
-------------------------------------------------------

-- HEAD_OFFICE (본부장)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_head01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_head01',@PASSWORD,N'본부장1','01010101','test_head01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_head02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_head02',@PASSWORD,N'본부장2','01010102','test_head02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_head03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_head03',@PASSWORD,N'본부장3','01010103','test_head03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_head04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_head04',@PASSWORD,N'본부장4','01010104','test_head04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_head05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_head05',@PASSWORD,N'본부장5','01010105','test_head05@test.jobmoa.com',1,0,0,GETDATE());

-- REGIONAL_MANAGER (지역담당자)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_region01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_region01',@PASSWORD,N'지역담당자1','01010201','test_region01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_region02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_region02',@PASSWORD,N'지역담당자2','01010202','test_region02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_region03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_region03',@PASSWORD,N'지역담당자3','01010203','test_region03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_region04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_region04',@PASSWORD,N'지역담당자4','01010204','test_region04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_region05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_region05',@PASSWORD,N'지역담당자5','01010205','test_region05@test.jobmoa.com',1,0,0,GETDATE());

-- OPERATOR (행정 → 배정 시 ADMIN_STAFF)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_oper01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_oper01',@PASSWORD,N'행정1','01010301','test_oper01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_oper02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_oper02',@PASSWORD,N'행정2','01010302','test_oper02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_oper03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_oper03',@PASSWORD,N'행정3','01010303','test_oper03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_oper04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_oper04',@PASSWORD,N'행정4','01010304','test_oper04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_oper05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_oper05',@PASSWORD,N'행정5','01010305','test_oper05@test.jobmoa.com',1,0,0,GETDATE());

-- COUNSELOR (상담사)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_counsel01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_counsel01',@PASSWORD,N'상담사1','01010401','test_counsel01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_counsel02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_counsel02',@PASSWORD,N'상담사2','01010402','test_counsel02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_counsel03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_counsel03',@PASSWORD,N'상담사3','01010403','test_counsel03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_counsel04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_counsel04',@PASSWORD,N'상담사4','01010404','test_counsel04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_counsel05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_counsel05',@PASSWORD,N'상담사5','01010405','test_counsel05@test.jobmoa.com',1,0,0,GETDATE());

-- LECTURER (강사)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_lect01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_lect01',@PASSWORD,N'강사1','01010501','test_lect01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_lect02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_lect02',@PASSWORD,N'강사2','01010502','test_lect02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_lect03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_lect03',@PASSWORD,N'강사3','01010503','test_lect03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_lect04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_lect04',@PASSWORD,N'강사4','01010504','test_lect04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_lect05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_lect05',@PASSWORD,N'강사5','01010505','test_lect05@test.jobmoa.com',1,0,0,GETDATE());

-- STAFF (진행요원)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_staff01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_staff01',@PASSWORD,N'진행요원1','01010601','test_staff01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_staff02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_staff02',@PASSWORD,N'진행요원2','01010602','test_staff02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_staff03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_staff03',@PASSWORD,N'진행요원3','01010603','test_staff03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_staff04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_staff04',@PASSWORD,N'진행요원4','01010604','test_staff04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_staff05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_staff05',@PASSWORD,N'진행요원5','01010605','test_staff05@test.jobmoa.com',1,0,0,GETDATE());

-- PROJECT_LEADER (PL)
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_pl01') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_pl01',@PASSWORD,N'PL1','01010801','test_pl01@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_pl02') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_pl02',@PASSWORD,N'PL2','01010802','test_pl02@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_pl03') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_pl03',@PASSWORD,N'PL3','01010803','test_pl03@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_pl04') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_pl04',@PASSWORD,N'PL4','01010804','test_pl04@test.jobmoa.com',1,0,0,GETDATE());
IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='test_pl05') INSERT INTO users (login_id,password,name,phone,email,enabled,locked,deleted,created_at) VALUES ('test_pl05',@PASSWORD,N'PL5','01010805','test_pl05@test.jobmoa.com',1,0,0,GETDATE());

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
