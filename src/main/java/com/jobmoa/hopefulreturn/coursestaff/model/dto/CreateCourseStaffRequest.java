package com.jobmoa.hopefulreturn.coursestaff.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "강좌 담당자 등록 요청")
public record CreateCourseStaffRequest(
        @Schema(description = "강좌 ID", example = "15")
        @NotNull
        Long courseId,

        @Schema(description = "사용자 ID", example = "3")
        @NotNull
        Long userId,

        @Schema(description = "담당 역할", example = "COUNSELOR")
        @NotBlank
        String staffRole,

        @Schema(description = "세션 유형", example = "FULL")
        @NotBlank
        String sessionType
) {
}
