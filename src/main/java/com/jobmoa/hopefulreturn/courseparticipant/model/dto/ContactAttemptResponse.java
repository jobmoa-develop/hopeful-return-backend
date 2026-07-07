package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "연락 시도 횟수 증가 응답")
public record ContactAttemptResponse(
        @Schema(description = "증가 후 연락 시도 횟수", example = "4")
        Integer contactAttempt
) {
}
