package com.jobmoa.hopefulreturn.coursestaff.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강좌 담당자 등록 응답")
public record CreateCourseStaffResponse(
        @Schema(description = "강좌 담당자 ID", example = "8")
        Long courseStaffId
) {
}
