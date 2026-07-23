package com.jobmoa.hopefulreturn.followup.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사후관리 수정 응답")
public record UpdateFollowUpResponse(
        @Schema(description = "사후관리 ID", example = "11")
        Long followUpId,

        @Schema(description = "수정일시", example = "2026-09-25T14:10:35")
        LocalDateTime updatedAt
) {
}
