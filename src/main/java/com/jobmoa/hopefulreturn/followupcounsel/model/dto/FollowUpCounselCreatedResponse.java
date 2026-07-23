package com.jobmoa.hopefulreturn.followupcounsel.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사후관리 상담 등록 응답")
public record FollowUpCounselCreatedResponse(
        @Schema(description = "사후관리 상담 ID", example = "7")
        Long followUpCounselId,

        @Schema(description = "수강생 ID", example = "15")
        Long courseParticipantId,

        @Schema(description = "상담 회차", example = "1")
        Integer counselNumber,

        @Schema(description = "등록일시", example = "2026-09-24T15:00:30")
        LocalDateTime createdAt
) {
}
