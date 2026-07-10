package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강좌 수정 응답")
public record UpdateCourseResponse(
        @Schema(description = "강좌 ID", example = "15")
        Long courseId,

        @Schema(description = "Local course number", example = "2")
        Integer localCourseNumber,

        @Schema(description = "수정 여부", example = "true")
        boolean updated
) {
}
