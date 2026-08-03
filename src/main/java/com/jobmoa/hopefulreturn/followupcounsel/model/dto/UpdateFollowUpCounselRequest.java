package com.jobmoa.hopefulreturn.followupcounsel.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "사후관리 상담 수정 요청")
public record UpdateFollowUpCounselRequest(
        @Schema(description = "상담일", example = "2026-09-25")
        LocalDate counselDate,

        @Schema(description = "상담 방식 — landline(유선)/text(문자)/offline(대면)", example = "text")
        String counselStatus,

        @Schema(description = "상담 메모", example = "문자 발송 완료")
        String counselMemo
) {
}
