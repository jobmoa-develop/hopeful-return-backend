-- V18: 대시보드 일정 체크(완료 표시) 영속화 — 전역 공유
-- 대시보드 "전체 일정" 체크박스는 대시보드 접근 권한이 있는 모든 사용자에게 공유된다.
-- (한 사람이 체크하면 다른 사람 화면에도 완료로 보여야 함) → task_id 단위로 유일하게 관리.

CREATE TABLE dashboard_task_completion (
    dashboard_task_completion_id BIGINT IDENTITY(1,1) NOT NULL,
    task_id       NVARCHAR(150) NOT NULL,
    completed_by  BIGINT NULL,       -- 누가 체크했는지(참고/감사용, NULL 허용)
    completed_at  DATETIME2 NOT NULL,
    CONSTRAINT PK_DASHBOARD_TASK_COMPLETION PRIMARY KEY (dashboard_task_completion_id)
);

ALTER TABLE dashboard_task_completion
    ADD CONSTRAINT FK_DASHBOARD_TASK_COMPLETION_COMPLETED_BY
    FOREIGN KEY (completed_by) REFERENCES users (user_id);

-- task_id 하나당 완료 레코드는 하나만(전역 공유이므로 사용자 구분 없음)
CREATE UNIQUE INDEX UQ_DASHBOARD_TASK_COMPLETION_TASK
    ON dashboard_task_completion (task_id);