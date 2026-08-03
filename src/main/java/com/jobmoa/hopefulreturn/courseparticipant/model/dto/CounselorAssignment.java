package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "상담사 배정 항목 (상담사 + 상담 구분 슬롯, 슬롯당 1명)")
public record CounselorAssignment(
        @Schema(description = "상담사(사용자) ID", example = "8")
        @NotNull
        Long counselorId,

        @Schema(description = "상담 구분 — PRE_SESSION(사전상담) / POST_SESSION_1(사후상담1) / POST_SESSION_2(사후상담2)",
                example = "PRE_SESSION")
        @NotNull
        String status
) {
}
