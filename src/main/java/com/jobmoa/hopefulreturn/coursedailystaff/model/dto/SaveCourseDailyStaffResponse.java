package com.jobmoa.hopefulreturn.coursedailystaff.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회차 날짜별 인력 배정 저장 응답")
public record SaveCourseDailyStaffResponse(
        @Schema(description = "회차 ID", example = "15")
        Long courseId,

        @Schema(description = "저장된 배정 건수", example = "12")
        int saved
) {
}
