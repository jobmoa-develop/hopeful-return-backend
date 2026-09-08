-- 상담 슬롯별 '상담 불가' 여부 플래그.
-- true(1)면 사유(시각·메모)와 무관하게 해당 상담을 '상담 불가'로 표기·판정한다.
-- 기존 행은 DEFAULT 0(=상담 가능)으로 채운다.
ALTER TABLE course_participant_counselor
    ADD unavailable BIT NOT NULL CONSTRAINT DF_CPC_unavailable DEFAULT 0;
