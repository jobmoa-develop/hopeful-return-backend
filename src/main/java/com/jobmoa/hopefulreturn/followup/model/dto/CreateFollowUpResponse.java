package com.jobmoa.hopefulreturn.followup.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사후관리 등록 응답")
public record CreateFollowUpResponse(
        @Schema(description = "사후관리 ID", example = "11")
        Long followUpId,

        @Schema(description = "수강생 ID", example = "15")
        Long courseParticipantId,

        @Schema(description = "등록일시", example = "2026-09-24T15:00:30")
        LocalDateTime createdAt
) {
}
