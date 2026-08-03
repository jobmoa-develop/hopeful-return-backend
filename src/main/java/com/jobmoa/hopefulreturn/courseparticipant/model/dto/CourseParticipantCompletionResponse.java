package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수료 처리 응답")
public record CourseParticipantCompletionResponse(
        @Schema(description = "수강 정보 ID", example = "101")
        Long courseParticipantId,

        @Schema(description = "수강 상태", example = "COMPLETED")
        String status
) {
}
