package com.jobmoa.hopefulreturn.attendanceleave.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

@Schema(description = "조퇴·외출 수정 요청 (부분 수정 — null 필드는 미변경)")
public record UpdateAttendanceLeaveRequest(
        @Schema(description = "외출(조퇴) 시각", example = "14:20:00")
        LocalTime leaveTime,

        @Schema(description = "복귀 시각", example = "15:15:00")
        LocalTime returnTime,

        @Schema(description = "사유", example = "병원 진료(시간 수정)")
        @Size(max = 255)
        String reason
) {
}
