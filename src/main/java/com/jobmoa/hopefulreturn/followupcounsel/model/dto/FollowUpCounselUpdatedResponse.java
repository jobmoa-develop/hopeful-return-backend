package com.jobmoa.hopefulreturn.followupcounsel.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사후관리 상담 수정 응답")
public record FollowUpCounselUpdatedResponse(
        @Schema(description = "사후관리 상담 ID", example = "7")
        Long followUpCounselId,

        @Schema(description = "수정일시", example = "2026-09-25T14:10:35")
        LocalDateTime updatedAt
) {
}
