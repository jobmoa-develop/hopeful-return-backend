package com.jobmoa.hopefulreturn.participant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참여자 삭제 응답")
public record ParticipantDeletedResponse(
        @Schema(description = "삭제 여부", example = "true")
        boolean deleted
) {
}
