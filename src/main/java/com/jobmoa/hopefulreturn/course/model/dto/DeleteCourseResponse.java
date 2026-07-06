package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강좌 삭제 응답")
public record DeleteCourseResponse(
        @Schema(description = "삭제 여부", example = "true")
        boolean deleted
) {
}
