package com.jobmoa.hopefulreturn.participantmemo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상담 메모 수정 응답")
public record ParticipantMemoUpdatedResponse(
        @Schema(description = "상담 메모 ID", example = "8")
        Long memoId,

        @Schema(description = "수정 여부", example = "true")
        boolean updated
) {
}
