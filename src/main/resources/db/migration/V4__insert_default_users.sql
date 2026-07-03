-------------------------------------------------------
-- Default Users
-------------------------------------------------------

DECLARE @PASSWORD NVARCHAR(255);
SET @PASSWORD = '$2a$10$xPyXJ8Zu57M0tOKmdDUZEuzVfclTHe6oYBvt2hiAtQrdU2GO2hHUy';

IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='admin01')
INSERT INTO users
(login_id,password,name,phone,email,enabled,locked,deleted,created_at)
VALUES
('admin01',@PASSWORD,N'관리자','01000000000','admin@jobmoa.com',1,0,0,GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='head01')
INSERT INTO users
(login_id,password,name,phone,email,enabled,locked,deleted,created_at)
VALUES
('head01',@PASSWORD,N'이인철','01045871737','leeic@jobmoa.com',1,0,0,GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='regionManager01')
INSERT INTO users
(login_id,password,name,phone,email,enabled,locked,deleted,created_at)
VALUES
('regionManager01',@PASSWORD,N'인천지역담당자','01011111111','inch@jobmoa.com',1,0,0,GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='regionManager02')
INSERT INTO users
(login_id,password,name,phone,email,enabled,locked,deleted,created_at)
VALUES
('regionManager02',@PASSWORD,N'양천지역담당자','01022222222','yang@jobmoa.com',1,0,0,GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='regionManager03')
INSERT INTO users
(login_id,password,name,phone,email,enabled,locked,deleted,created_at)
VALUES
('regionManager03',@PASSWORD,N'강북지역담당자','01033333333','gangb@jobmoa.com',1,0,0,GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='oper01')
INSERT INTO users
(login_id,password,name,phone,email,enabled,locked,deleted,created_at)
VALUES
('oper01',@PASSWORD,N'한준희','0215665011','hanjh@jobmoa.com',1,0,0,GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='counsel01')
INSERT INTO users
(login_id,password,name,phone,email,enabled,locked,deleted,created_at)
VALUES
('counsel01',@PASSWORD,N'상담사1','01044444444','consel@jobmoa.com',1,0,0,GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='lect01')
INSERT INTO users
(login_id,password,name,phone,email,enabled,locked,deleted,created_at)
VALUES
('lect01',@PASSWORD,N'강사1','01055555555','lect@jobmoa.com',1,0,0,GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE login_id='staff01')
INSERT INTO users
(login_id,password,name,phone,email,enabled,locked,deleted,created_at)
VALUES
('staff01',@PASSWORD,N'진행요원1','01066666666','staff1@jobmoa.com',1,0,0,GETDATE());