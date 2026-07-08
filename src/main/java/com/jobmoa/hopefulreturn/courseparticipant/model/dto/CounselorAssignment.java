package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "상담사 배정 항목 (상담사 + 사전/사후 구분)")
public record CounselorAssignment(
        @Schema(description = "상담사(사용자) ID", example = "8")
        @NotNull
        Long counselorId,

        @Schema(description = "상담 구분 — PRE(사전상담) / POST(사후상담)", example = "PRE")
        @NotNull
        String status
) {
}
