-- V15: 문자 발송결과(실제 전달) 추적 컬럼
-- SENS 202 는 '접수' 성공일 뿐이라, 발송결과 조회 API 폴링으로 실제 전달 상태를 반영한다.
-- request_id: 발송 요청 단위 ID(SENS). message_id: 수신 건별 ID(결과 조회로 확보, 메시지 검색용).
-- result_code/result_message: 결과 조회의 statusCode/statusName·statusMessage(실패 사유). complete_time: 전달 완료 시각.

ALTER TABLE participant_sms ADD
    request_id NVARCHAR(50) NULL,
    message_id NVARCHAR(50) NULL,
    result_code NVARCHAR(20) NULL,
    result_message NVARCHAR(200) NULL,
    complete_time DATETIME2 NULL;
