package com.jobmoa.hopefulreturn.adminsms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 취소 사전 확인 응답. SENS 예약 취소는 reserveId(batch) 단위라 이 예약에 묶인 전 수신자가 함께 취소된다.
 * FE 는 이 정보를 모달로 보여 "함께 취소될 대상"을 사용자가 인지한 뒤 취소하도록 한다.
 */
@Schema(description = "예약 취소 사전 확인(함께 취소될 대상)")
public record AdminReservationCancelPreviewResponse(
        @Schema(description = "SENS 예약 batch ID", example = "0-Reserve-...")
        String reserveId,

        @Schema(description = "함께 취소될(예약중) 인원 수", example = "12")
        int targetCount,

        @Schema(description = "예약 발송 예정 시각", example = "2026-09-10T09:00:00")
        LocalDateTime reserveTime,

        @Schema(description = "함께 취소될 대상 수신 전화번호 목록", example = "[\"01012345678\", \"01098765432\"]")
        List<String> recipients
) {
}
