package com.jobmoa.hopefulreturn.participant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "참여자 등록 요청")
public record CreateParticipantRequest(
        @Schema(description = "참여자명", example = "김철수")
        @NotBlank
        @Size(max = 50)
        String name,

        @Schema(description = "출생연도", example = "1978")
        Integer birthYear,

        @Schema(description = "전화번호", example = "010-5678-1234")
        @NotBlank
        @Size(max = 20)
        String phone
) {
}
