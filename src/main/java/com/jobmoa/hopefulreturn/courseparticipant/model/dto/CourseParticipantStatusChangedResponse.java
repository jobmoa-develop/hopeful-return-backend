package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "진행상태 변경 응답")
public record CourseParticipantStatusChangedResponse(
        @Schema(description = "수강 정보 ID", example = "101")
        Long courseParticipantId,

        @Schema(description = "변경된 진행상태", example = "CONFIRMED")
        String status
) {
}
