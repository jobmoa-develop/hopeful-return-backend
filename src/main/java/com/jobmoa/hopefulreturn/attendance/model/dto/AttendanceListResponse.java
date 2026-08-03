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

            @Schema(description = "수강 정보 ID", example = "15")
            Long courseParticipantId,

            @Schema(description = "참여자명", example = "김철수")
            String participantName,

            @Schema(description = "수업 차수(일차)", example = "1")
            Integer dayNo,

            @Schema(description = "입실 시각", example = "08:55:23")
            LocalTime checkInTime,

            @Schema(description = "퇴실 시각", example = "18:02:10")
            LocalTime checkOutTime,

            @Schema(description = "출결 상태", example = "ATTEND")
            String status,

            @Schema(description = "조퇴·외출 기록 목록")
            List<LeaveItem> leaves
    ) {
    }

    @Schema(description = "출석 항목의 조퇴·외출 기록")
    public record LeaveItem(
            @Schema(description = "조퇴·외출 ID", example = "5")
            Long attendanceLeaveId,

            @Schema(description = "외출(조퇴) 시각", example = "14:30:00")
            LocalTime leaveTime,

            @Schema(description = "복귀 시각", example = "15:20:00")
            LocalTime returnTime,

            @Schema(description = "사유", example = "병원 진료")
            String reason
    ) {
    }
}
