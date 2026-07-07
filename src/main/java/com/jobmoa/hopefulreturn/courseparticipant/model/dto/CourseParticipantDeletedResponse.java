package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수강 삭제 응답")
public record CourseParticipantDeletedResponse(
        @Schema(description = "삭제 성공 여부", example = "true")
        boolean deleted
) {
}
