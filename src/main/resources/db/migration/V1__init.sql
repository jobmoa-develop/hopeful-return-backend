CREATE TABLE region (
    region_id BIGINT NOT NULL,
    parent_region_id BIGINT NULL,
    name NVARCHAR(50) NOT NULL,
    level NVARCHAR(50) NOT NULL,
    CONSTRAINT PK_REGION PRIMARY KEY (region_id)
);

CREATE TABLE users (
    user_id BIGINT NOT NULL,
    login_id NVARCHAR(100) NOT NULL,
    password NVARCHAR(255) NOT NULL,
    name NVARCHAR(50) NOT NULL,
    phone NVARCHAR(20) NULL,
    email NVARCHAR(100) NULL,
    enabled BIT NOT NULL,
    locked BIT NULL,
    deleted BIT NULL,
    created_at DATETIME2 NULL,
    updated_at DATETIME2 NULL,
    CONSTRAINT PK_USERS PRIMARY KEY (user_id)
);

CREATE TABLE role (
    role_id BIGINT NOT NULL,
    role_name NVARCHAR(50) NOT NULL,
    description NVARCHAR(255) NULL,
    CONSTRAINT PK_ROLE PRIMARY KEY (role_id)
);

CREATE TABLE participant (
    participant_id BIGINT NOT NULL,
    name NVARCHAR(50) NOT NULL,
    birth_year INT NULL,
    phone NVARCHAR(20) NOT NULL,
    match_key NVARCHAR(100) NULL,
    created_at DATETIME2 NULL,
    updated_at DATETIME2 NULL,
    CONSTRAINT PK_PARTICIPANT PRIMARY KEY (participant_id)
);

CREATE TABLE course (
    course_id BIGINT NOT NULL,
    region_id BIGINT NOT NULL,
    course_number INT NOT NULL,
    course_name NVARCHAR(100) NULL,
    recruit_start DATE NULL,
    recruit_end DATE NULL,
    day1_date DATE NULL,
    day2_date DATE NULL,
    day3_date DATE NULL,
    day4_date DATE NULL,
    day5_date DATE NULL,
    capacity INT NOT NULL,
    minimum_capacity INT NOT NULL,
    location NVARCHAR(100) NULL,
    status NVARCHAR(50) NULL,
    plan_submit_date DATE NULL,
    created_by BIGINT NULL,
    created_at DATETIME2 NULL,
    updated_at DATETIME2 NULL,
    education_start_time TIME NULL,
    education_end_time TIME NULL,
    CONSTRAINT PK_COURSE PRIMARY KEY (course_id)
);

CREATE TABLE course_participant (
    course_participant_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    counselor_id BIGINT NULL,
    inflow_type NVARCHAR(30) NULL,
    apply_date DATE NULL,
    reception_date DATE NULL,
    basic_education NVARCHAR(20) NULL,
    status NVARCHAR(50) NULL,
    contact_attempt INT NULL,
    completion_date DATE NULL,
    incomplete_reason NVARCHAR(255) NULL,
    allowance_paid BIT NULL,
    allowance_date DATE NULL,
    created_at DATETIME2 NULL,
    updated_at DATETIME2 NULL,
    CONSTRAINT PK_COURSE_PARTICIPANT PRIMARY KEY (course_participant_id)
);

CREATE TABLE course_staff (
    course_staff_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    staff_role NVARCHAR(50) NULL,
    session_type NVARCHAR(50) NULL,
    created_at DATETIME2 NULL,
    CONSTRAINT PK_COURSE_STAFF PRIMARY KEY (course_staff_id)
);

CREATE TABLE user_role (
    user_role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT PK_USER_ROLE PRIMARY KEY (user_role_id)
);

CREATE TABLE allowance (
    allowance_id BIGINT NOT NULL,
    course_participant_id BIGINT NOT NULL,
    amount INT NULL,
    paid BIT NULL,
    paid_date DATE NULL,
    remark NVARCHAR(255) NULL,
    CONSTRAINT PK_ALLOWANCE PRIMARY KEY (allowance_id)
);

CREATE TABLE attendance (
    attendance_id BIGINT NOT NULL,
    course_participant_id BIGINT NOT NULL,
    day_no INT NOT NULL,
    check_in_time TIME NULL,
    check_out_time TIME NULL,
    status NVARCHAR(50) NULL,
    created_at DATETIME2 NULL,
    CONSTRAINT PK_ATTENDANCE PRIMARY KEY (attendance_id)
);

CREATE TABLE attendance_leave (
    attendance_leave_id BIGINT NOT NULL,
    attendance_id BIGINT NOT NULL,
    leave_time TIME NULL,
    return_time TIME NULL,
    reason NVARCHAR(255) NULL,
    created_at DATETIME2 NULL,
    CONSTRAINT PK_ATTENDANCE_LEAVE PRIMARY KEY (attendance_leave_id)
);

CREATE TABLE follow_up (
    followup_id BIGINT NOT NULL,
    course_participant_id BIGINT NOT NULL,
    month_no INT NULL,
    contact_date DATE NULL,
    contact_type NVARCHAR(30) NULL,
    employment_status NVARCHAR(30) NULL,
    national_program NVARCHAR(30) NULL,
    forest_program BIT NULL,
    remark NVARCHAR(255) NULL,
    CONSTRAINT PK_FOLLOW_UP PRIMARY KEY (followup_id)
);

CREATE TABLE participant_memo (
    memo_id BIGINT NOT NULL,
    course_participant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content NVARCHAR(255) NULL,
    created_at DATETIME2 NULL,
    CONSTRAINT PK_PARTICIPANT_MEMO PRIMARY KEY (memo_id)
);

CREATE TABLE participant_sms (
    sms_id BIGINT NOT NULL,
    sent_by BIGINT NULL,
    sms_type NVARCHAR(50) NULL,
    title NVARCHAR(100) NULL,
    content NVARCHAR(255) NULL,
    send_status NVARCHAR(50) NULL,
    sent_at DATETIME2 NULL,
    created_at DATETIME2 NULL,
    course_participant_id BIGINT NOT NULL,
    CONSTRAINT PK_PARTICIPANT_SMS PRIMARY KEY (sms_id)
);

ALTER TABLE region
ADD CONSTRAINT FK_REGION_PARENT_REGION
FOREIGN KEY (parent_region_id) REFERENCES region (region_id);

ALTER TABLE course
ADD CONSTRAINT FK_COURSE_REGION
FOREIGN KEY (region_id) REFERENCES region (region_id);

ALTER TABLE course
ADD CONSTRAINT FK_COURSE_CREATED_BY_USERS
FOREIGN KEY (created_by) REFERENCES users (user_id);

ALTER TABLE course_participant
ADD CONSTRAINT FK_COURSE_PARTICIPANT_COURSE
FOREIGN KEY (course_id) REFERENCES course (course_id);

ALTER TABLE course_participant
ADD CONSTRAINT FK_COURSE_PARTICIPANT_PARTICIPANT
FOREIGN KEY (participant_id) REFERENCES participant (participant_id);

ALTER TABLE course_participant
ADD CONSTRAINT FK_COURSE_PARTICIPANT_COUNSELOR_USERS
FOREIGN KEY (counselor_id) REFERENCES users (user_id);

ALTER TABLE course_staff
ADD CONSTRAINT FK_COURSE_STAFF_COURSE
FOREIGN KEY (course_id) REFERENCES course (course_id);

ALTER TABLE course_staff
ADD CONSTRAINT FK_COURSE_STAFF_USERS
FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE user_role
ADD CONSTRAINT FK_USER_ROLE_USERS
FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE user_role
ADD CONSTRAINT FK_USER_ROLE_ROLE
FOREIGN KEY (role_id) REFERENCES role (role_id);

ALTER TABLE allowance
ADD CONSTRAINT FK_ALLOWANCE_COURSE_PARTICIPANT
FOREIGN KEY (course_participant_id) REFERENCES course_participant (course_participant_id);

ALTER TABLE attendance
ADD CONSTRAINT FK_ATTENDANCE_COURSE_PARTICIPANT
FOREIGN KEY (course_participant_id) REFERENCES course_participant (course_participant_id);

ALTER TABLE attendance_leave
ADD CONSTRAINT FK_ATTENDANCE_LEAVE_ATTENDANCE
FOREIGN KEY (attendance_id) REFERENCES attendance (attendance_id);

ALTER TABLE follow_up
ADD CONSTRAINT FK_FOLLOW_UP_COURSE_PARTICIPANT
FOREIGN KEY (course_participant_id) REFERENCES course_participant (course_participant_id);

ALTER TABLE participant_memo
ADD CONSTRAINT FK_PARTICIPANT_MEMO_COURSE_PARTICIPANT
FOREIGN KEY (course_participant_id) REFERENCES course_participant (course_participant_id);

ALTER TABLE participant_memo
ADD CONSTRAINT FK_PARTICIPANT_MEMO_USERS
FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE participant_sms
ADD CONSTRAINT FK_PARTICIPANT_SMS_SENT_BY_USERS
FOREIGN KEY (sent_by) REFERENCES users (user_id);

ALTER TABLE participant_sms
ADD CONSTRAINT FK_PARTICIPANT_SMS_COURSE_PARTICIPANT
FOREIGN KEY (course_participant_id) REFERENCES course_participant (course_participant_id);
