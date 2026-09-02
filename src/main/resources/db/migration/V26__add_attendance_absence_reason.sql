-- V26: 출결 수기 입력 시 '결석' 사유 저장 컬럼 추가
-- 출결 현장에서 결석을 명시적으로 체크하고 사유를 남길 수 있도록 한다.
-- (status 는 입실시간으로 자동 판정되지만, 결석 사유 텍스트를 담을 곳이 없어 신규 컬럼을 추가)
-- 값이 없을 수 있으므로 nullable.

ALTER TABLE attendance
    ADD absence_reason NVARCHAR(255) NULL;
