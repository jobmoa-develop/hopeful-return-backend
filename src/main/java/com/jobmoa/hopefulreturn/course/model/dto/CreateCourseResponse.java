package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강좌 등록 응답")
public record CreateCourseResponse(
        @Schema(description = "강좌 ID", example = "15")
        Long courseId,

        @Schema(description = "강좌 상태", example = "PLANNED")
        String status
) {
}
