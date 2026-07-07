package com.jobmoa.hopefulreturn.participantmemo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상담 메모 삭제 응답")
public record ParticipantMemoDeletedResponse(
        @Schema(description = "삭제 결과 메시지", example = "상담 메모가 삭제되었습니다.")
        String message
) {
}
