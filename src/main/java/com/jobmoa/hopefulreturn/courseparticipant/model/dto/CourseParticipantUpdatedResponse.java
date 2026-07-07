package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수강 정보 수정 응답")
public record CourseParticipantUpdatedResponse(
        @Schema(description = "수정 성공 여부", example = "true")
        boolean updated
) {
}
