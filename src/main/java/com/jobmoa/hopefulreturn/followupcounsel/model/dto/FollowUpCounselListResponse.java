package com.jobmoa.hopefulreturn.followupcounsel.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "사후관리 상담 목록 응답")
public record FollowUpCounselListResponse(
        List<Item> content
) {

    @Schema(description = "사후관리 상담 목록 항목")
    public record Item(
            @Schema(description = "사후관리 상담 ID", example = "7")
            Long followUpCounselId,

            @Schema(description = "상담 회차", example = "1")
            Integer counselNumber,

            @Schema(description = "상담일", example = "2026-09-24")
            LocalDate counselDate,

            @Schema(description = "상담 방식 — landline/text/offline", example = "landline")
            String counselStatus,

            @Schema(description = "상담 메모", example = "현재 구직 중")
            String counselMemo
    ) {
    }
}
