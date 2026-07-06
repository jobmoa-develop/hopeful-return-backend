package com.jobmoa.hopefulreturn.participant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참여자 수정 응답")
public record ParticipantUpdatedResponse(
        @Schema(description = "참여자 ID", example = "25")
        Long participantId,

        @Schema(description = "수정 여부", example = "true")
        boolean updated
) {
}
