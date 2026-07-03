-------------------------------------------------------
-- Role Master Data
-------------------------------------------------------

IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'ADMIN')
BEGIN
    INSERT INTO role (role_name, description)
    VALUES ('ADMIN', N'시스템 관리자');
END;

IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'HEAD_OFFICE')
BEGIN
    INSERT INTO role (role_name, description)
    VALUES ('HEAD_OFFICE', N'본부장');
END;

IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'REGIONAL_MANAGER')
BEGIN
    INSERT INTO role (role_name, description)
    VALUES ('REGIONAL_MANAGER', N'지역담당자');
END;

IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'OPERATOR')
BEGIN
    INSERT INTO role (role_name, description)
    VALUES ('OPERATOR', N'행정허브');
END;

IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'COUNSELOR')
BEGIN
    INSERT INTO role (role_name, description)
    VALUES ('COUNSELOR', N'상담사');
END;

IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'LECTURER')
BEGIN
    INSERT INTO role (role_name, description)
    VALUES ('LECTURER', N'강사');
END;

IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'STAFF')
BEGIN
    INSERT INTO role (role_name, description)
    VALUES ('STAFF', N'진행요원');
END;