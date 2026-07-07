package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "상담사 변경 요청")
public record ChangeCounselorRequest(
        @Schema(description = "변경할 상담사(사용자) ID", example = "12")
        @NotNull
        Long counselorId
) {
}
