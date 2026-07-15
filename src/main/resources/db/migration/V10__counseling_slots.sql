-- V10: 상담 슬롯 3분화 + 상담 세션 기록 컬럼
-- 상담 구분을 PRE/POST 2값에서 PRE_SESSION(사전상담)/POST_SESSION_1(사후상담1)/POST_SESSION_2(사후상담2)
-- 3슬롯으로 확장하고, 상담 완료 판정(종료 일시 입력 시 완료)을 위한 기록 컬럼을 추가한다.

-- 1) 기존 값 이관: PRE→PRE_SESSION, POST→POST_SESSION_1
UPDATE course_participant_counselor SET status = 'PRE_SESSION'    WHERE status = 'PRE';
UPDATE course_participant_counselor SET status = 'POST_SESSION_1' WHERE status = 'POST';

-- 2) 방어: 같은 (수강건, 슬롯)에 상담사가 여럿이면 최소 id만 남긴다
--    (구 제약은 상담사까지 포함해 슬롯당 여러 상담사를 허용했음)
DELETE c FROM course_participant_counselor c
WHERE EXISTS (
    SELECT 1 FROM course_participant_counselor d
    WHERE d.course_participant_id = c.course_participant_id
      AND d.status = c.status
      AND d.course_participant_counselor_id < c.course_participant_counselor_id
);

-- 3) 유니크 제약 교체: (수강건, 상담사, 슬롯) → (수강건, 슬롯) — 슬롯당 상담사 1명
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'UQ_CPC_PARTICIPANT_COUNSELOR_STATUS')
    ALTER TABLE course_participant_counselor DROP CONSTRAINT UQ_CPC_PARTICIPANT_COUNSELOR_STATUS;

ALTER TABLE course_participant_counselor
    ADD CONSTRAINT UQ_CPC_PARTICIPANT_STATUS UNIQUE (course_participant_id, status);

-- 4) 상담 세션 기록 컬럼 (완료 = counseling_ended_at IS NOT NULL)
ALTER TABLE course_participant_counselor ADD
    counseling_started_at DATETIME2 NULL,
    counseling_ended_at   DATETIME2 NULL,
    counseling_memo       NVARCHAR(1000) NULL;
