-- V24: 회원 직책(position) 컬럼 추가
-- 근무기록표 인쇄의 '직책' 컬럼(예: 대표이사/상무이사/책임/선임/전문위원)에 사용한다.
-- 회원관리에서 편집하며, 값이 없을 수 있으므로 nullable.
-- (position 은 SQL Server 예약어이므로 대괄호로 감싼다)

ALTER TABLE users
    ADD [position] NVARCHAR(50) NULL;
