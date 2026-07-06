package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "강좌 상태 변경 요청")
public record UpdateCourseStatusRequest(
        @Schema(description = "강좌 상태", example = "OPEN")
        @NotBlank
        String status
) {
}
