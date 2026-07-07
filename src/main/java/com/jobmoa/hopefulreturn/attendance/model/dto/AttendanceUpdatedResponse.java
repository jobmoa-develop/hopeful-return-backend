package com.jobmoa.hopefulreturn.attendance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "출석 수정 응답")
public record AttendanceUpdatedResponse(
        @Schema(description = "출석 ID", example = "31")
        Long attendanceId,

        @Schema(description = "출결 상태", example = "LATE")
        String status,

        @Schema(description = "수정 시각(응답 시점 계산 — attendance 테이블에 updated_at 컬럼 없음)", example = "2026-08-18T09:10:20")
        LocalDateTime updatedAt
) {
}
