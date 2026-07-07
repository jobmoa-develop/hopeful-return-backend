package com.jobmoa.hopefulreturn.participantmemo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "상담 메모 등록 요청")
public record CreateParticipantMemoRequest(
        @Schema(description = "수강생 ID", example = "15")
        @NotNull
        Long courseParticipantId,

        @Schema(description = "상담 메모 내용", example = "금일 유선 상담 완료.")
        @NotBlank
        String content
) {
}
