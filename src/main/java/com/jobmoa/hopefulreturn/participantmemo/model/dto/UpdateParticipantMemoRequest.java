package com.jobmoa.hopefulreturn.participantmemo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "상담 메모 수정 요청")
public record UpdateParticipantMemoRequest(
        @Schema(description = "상담 메모 내용", example = "금일 유선 상담 완료.")
        @NotBlank
        String content
) {
}
