package com.jobmoa.hopefulreturn.coursestaff.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강좌 담당자 수정 응답")
public record UpdateCourseStaffResponse(
        @Schema(description = "수정 여부", example = "true")
        boolean updated
) {
}
