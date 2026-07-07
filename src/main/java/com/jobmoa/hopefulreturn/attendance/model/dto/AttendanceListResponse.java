package com.jobmoa.hopefulreturn.attendance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "출석 목록 응답")
public record AttendanceListResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    @Schema(description = "출석 목록 항목")
    public record Item(
            @Schema(description = "출석 ID", example = "31")
            Long attendanceId,

            @Schema(description = "참여자명", example = "김철수")
            String participantName,

            @Schema(description = "수업 차수(일차)", example = "1")
            Integer dayNo,

            @Schema(description = "입실 시각", example = "08:55:23")
            LocalTime checkInTime,

            @Schema(description = "퇴실 시각", example = "18:02:10")
            LocalTime checkOutTime,

            @Schema(description = "출결 상태", example = "ATTEND")
            String status
    ) {
    }
}
