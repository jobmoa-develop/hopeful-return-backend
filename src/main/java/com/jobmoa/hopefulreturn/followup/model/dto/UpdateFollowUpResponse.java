package com.jobmoa.hopefulreturn.followup.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사후관리 수정 응답")
public record UpdateFollowUpResponse(
        @Schema(description = "사후관리 ID", example = "11")
        Long followUpId,

        @Schema(description = "수정 여부", example = "true")
        boolean updated
) {
}
