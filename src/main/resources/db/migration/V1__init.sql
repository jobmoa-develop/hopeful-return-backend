-- 회원(members) 초기 스키마
-- 한글 저장을 위해 NVARCHAR 사용 (use_nationalized_character_data=true 와 정합)
CREATE TABLE members (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    email          NVARCHAR(100) NOT NULL,
    password       NVARCHAR(255) NOT NULL,
    name           NVARCHAR(50)  NOT NULL,
    role           NVARCHAR(20)  NOT NULL,
    email_verified BIT           NOT NULL CONSTRAINT DF_members_email_verified DEFAULT (0),
    created_at     DATETIME2     NOT NULL,
    updated_at     DATETIME2     NULL,
    CONSTRAINT PK_members PRIMARY KEY (id),
    CONSTRAINT UQ_members_email UNIQUE (email)
);
