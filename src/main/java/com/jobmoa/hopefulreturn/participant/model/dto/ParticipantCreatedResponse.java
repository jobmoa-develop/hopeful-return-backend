package com.jobmoa.hopefulreturn.participant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참여자 등록 응답")
public record ParticipantCreatedResponse(
        @Schema(description = "생성된 참여자 ID", example = "25")
        Long participantId
) {
}
