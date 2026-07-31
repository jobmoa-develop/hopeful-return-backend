-- V17: 상담사/일정 변경 이력 테이블
-- 상담사 배정 변경(COUNSELOR_CHANGE)·상담 일정 변경(SCHEDULE_CHANGE) 시 변경 전·후 값과
-- 변경 주체(changed_by)·비고(reason)를 append-only 로 남긴다.
-- 감사 로그 성격이라 대상 삭제를 막지 않도록 별도 FK 제약은 두지 않고 조회용 인덱스만 둔다.

CREATE TABLE course_participant_counselor_history (
    history_id            BIGINT IDENTITY(1,1) NOT NULL,
    account_user_id       BIGINT       NULL,
    course_participant_id BIGINT       NOT NULL,
    course_number         INT          NULL,
    region_id             BIGINT       NULL,
    counseling_type       NVARCHAR(30) NULL,
    change_type           NVARCHAR(30) NOT NULL,
    old_counselor_id      BIGINT       NULL,
    new_counselor_id      BIGINT       NULL,
    changed_date          DATETIME2    NOT NULL,
    old_started_at        DATETIME2    NULL,
    new_started_at        DATETIME2    NULL,
    old_ended_at          DATETIME2    NULL,
    new_ended_at          DATETIME2    NULL,
    changed_by            NVARCHAR(20) NOT NULL,
    reason                NVARCHAR(500) NULL,
    created_at            DATETIME2    NOT NULL,
    CONSTRAINT PK_course_participant_counselor_history PRIMARY KEY (history_id)
);

CREATE INDEX IX_CPCH_COURSE_PARTICIPANT
    ON course_participant_counselor_history (course_participant_id);
