package com.jobmoa.hopefulreturn.followup.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "사후관리 목록 응답")
public record FollowUpListResponse(
        List<Item> content
) {

    @Schema(description = "사후관리 목록 항목")
    public record Item(
            @Schema(description = "사후관리 ID", example = "11")
            Long followUpId,

            @Schema(description = "사후관리 개월차", example = "1")
            Integer monthNo,

            @Schema(description = "상담일", example = "2026-09-24")
            LocalDate contactDate,

            @Schema(description = "취업 상태", example = "LOOKING")
            String employmentStatus,

            @Schema(description = "상담 유형", example = "PHONE")
            String contactType
    ) {
    }
}
