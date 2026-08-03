package com.jobmoa.hopefulreturn.coursestaff.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강좌 담당자 삭제 응답")
public record DeleteCourseStaffResponse(
        @Schema(description = "삭제 여부", example = "true")
        boolean deleted
) {
}
