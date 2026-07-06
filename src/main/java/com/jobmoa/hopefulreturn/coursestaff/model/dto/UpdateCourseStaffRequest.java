package com.jobmoa.hopefulreturn.coursestaff.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "강좌 담당자 수정 요청")
public record UpdateCourseStaffRequest(
        @Schema(description = "담당 역할", example = "LECTURER")
        @NotBlank
        String staffRole,

        @Schema(description = "세션 유형", example = "AM")
        @NotBlank
        String sessionType
) {
}
