package com.jobmoa.hopefulreturn.participantsms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 취소 사전 확인 응답. SENS 예약 취소는 reserveId(batch) 단위라 이 예약에 묶인 전 수신자가 함께 취소된다.
 * FE 는 이 정보를 모달로 보여 "함께 취소될 인원"을 사용자가 인지한 뒤 취소하도록 한다.
 */
@Schema(description = "예약 취소 사전 확인(함께 취소될 대상)")
public record ReservationCancelPreviewResponse(
        @Schema(description = "SENS 예약 batch ID", example = "0-Reserve-...")
        String reserveId,

        @Schema(description = "함께 취소될(예약중) 인원 수", example = "12")
        int targetCount,

        @Schema(description = "예약 발송 예정 시각", example = "2026-08-01T09:00:00")
        LocalDateTime reserveTime,

        @Schema(description = "함께 취소될 대상 수신자명 목록", example = "[\"홍길동\", \"김철수\"]")
        List<String> recipientNames
) {
}
