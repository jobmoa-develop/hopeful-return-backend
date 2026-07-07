package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수강 취소 응답")
public record CourseParticipantCanceledResponse(
        @Schema(description = "수강 상태", example = "CANCELED")
        String status
) {
}
