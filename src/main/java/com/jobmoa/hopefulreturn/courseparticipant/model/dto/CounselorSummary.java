package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상담사 배정 요약 (상담사 + 사전/사후 구분)")
public record CounselorSummary(
        @Schema(description = "상담사(사용자) ID", example = "8")
        Long counselorId,

        @Schema(description = "상담사명", example = "홍길동")
        String counselorName,

        @Schema(description = "상담 구분 — PRE(사전상담) / POST(사후상담)", example = "PRE")
        String status
) {
}
