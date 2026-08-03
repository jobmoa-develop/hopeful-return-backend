package com.jobmoa.hopefulreturn.attendanceleave.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "조퇴·외출 수정 응답")
public record AttendanceLeaveUpdatedResponse(
        @Schema(description = "조퇴·외출 ID", example = "5")
        Long attendanceLeaveId,

        @Schema(description = "수정 시각(응답 시점 계산 — attendance_leave 테이블에 updated_at 컬럼 없음)",
                example = "2026-08-18T15:25:10")
        LocalDateTime updatedAt
) {
}
