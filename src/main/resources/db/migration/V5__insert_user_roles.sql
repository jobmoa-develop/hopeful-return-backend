-------------------------------------------------------
-- User Role Mapping
-------------------------------------------------------

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name='ADMIN'
WHERE u.login_id='admin01'
AND NOT EXISTS (
    SELECT 1
    FROM user_role ur
    WHERE ur.user_id=u.user_id
      AND ur.role_id=r.role_id
);

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name='HEAD_OFFICE'
WHERE u.login_id='head01'
AND NOT EXISTS (
    SELECT 1
    FROM user_role ur
    WHERE ur.user_id=u.user_id
      AND ur.role_id=r.role_id
);

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name='REGIONAL_MANAGER'
WHERE u.login_id='regionManager01'
AND NOT EXISTS (
    SELECT 1
    FROM user_role ur
    WHERE ur.user_id=u.user_id
      AND ur.role_id=r.role_id
);

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name='REGIONAL_MANAGER'
WHERE u.login_id='regionManager02'
AND NOT EXISTS (
    SELECT 1
    FROM user_role ur
    WHERE ur.user_id=u.user_id
      AND ur.role_id=r.role_id
);

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name='REGIONAL_MANAGER'
WHERE u.login_id='regionManager03'
AND NOT EXISTS (
    SELECT 1
    FROM user_role ur
    WHERE ur.user_id=u.user_id
      AND ur.role_id=r.role_id
);

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name='OPERATOR'
WHERE u.login_id='oper01'
AND NOT EXISTS (
    SELECT 1
    FROM user_role ur
    WHERE ur.user_id=u.user_id
      AND ur.role_id=r.role_id
);

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name='COUNSELOR'
WHERE u.login_id='counsel01'
AND NOT EXISTS (
    SELECT 1
    FROM user_role ur
    WHERE ur.user_id=u.user_id
      AND ur.role_id=r.role_id
);

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name='LECTURER'
WHERE u.login_id='lect01'
AND NOT EXISTS (
    SELECT 1
    FROM user_role ur
    WHERE ur.user_id=u.user_id
      AND ur.role_id=r.role_id
);

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name='STAFF'
WHERE u.login_id='staff01'
AND NOT EXISTS (
    SELECT 1
    FROM user_role ur
    WHERE ur.user_id=u.user_id
      AND ur.role_id=r.role_id
);