package com.jobmoa.hopefulreturn.followupcounsel.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "사후관리 상담 등록 요청")
public record CreateFollowUpCounselRequest(
        @Schema(description = "수강생 ID", example = "15")
        @NotNull
        Long courseParticipantId,

        @Schema(description = "상담 회차", example = "1")
        Integer counselNumber,

        @Schema(description = "상담일", example = "2026-09-24")
        LocalDate counselDate,

        @Schema(description = "상담 방식 — landline(유선)/text(문자)/offline(대면)", example = "landline")
        String counselStatus,

        @Schema(description = "상담 메모", example = "현재 구직 중")
        String counselMemo
) {
}
