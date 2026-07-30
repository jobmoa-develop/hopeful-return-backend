-- V16: SMS 예약 발송(SENS 네이티브 reserveTime)
-- reserve_time: 예약 발송 예정 시각(예약건은 sent_at 대신 이 값으로 표시·정렬).
-- reserve_id: SENS 예약 batch ID — 예약 취소는 이 단위로 이루어진다(같은 reserve_id 전건 동시 취소).
-- send_status 는 NVARCHAR(50)·CHECK 제약 없음 → RESERVED/CANCELED 값 추가에 별도 변경 불필요.

ALTER TABLE participant_sms ADD
    reserve_time DATETIME2 NULL,
    reserve_id NVARCHAR(50) NULL;
