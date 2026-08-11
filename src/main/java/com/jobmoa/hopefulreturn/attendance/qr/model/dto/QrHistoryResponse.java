package com.jobmoa.hopefulreturn.attendance.qr.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * QR 본인 입·퇴실 내역(읽기전용) 응답 — 회차 전 일차의 출결/조퇴·외출 내역을 일차순으로 담는다.
 */
@Schema(description = "QR 본인 입·퇴실 내역(읽기전용) 응답")
public record QrHistoryResponse(
        @Schema(description = "참여자명", example = "김철수")
        String participantName,

        @Schema(description = "일차별 내역")
        List<DayItem> days
) {

    @Schema(description = "일차별 출결 내역")
    public record DayItem(
            @Schema(description = "교육 일차(1~5)", example = "1")
            Integer dayNo,

            @Schema(description = "해당 일차 날짜", example = "2026-08-12")
            LocalDate date,

            @Schema(description = "입실 시각", example = "09:03:00")
            LocalTime checkInTime,

            @Schema(description = "퇴실 시각", example = "18:01:00")
            LocalTime checkOutTime,

            @Schema(description = "출결 상태", example = "LATE")
            String status,

            @Schema(description = "조퇴·외출 기록 목록")
            List<QrLeaveItem> leaves
    ) {
    }
}
