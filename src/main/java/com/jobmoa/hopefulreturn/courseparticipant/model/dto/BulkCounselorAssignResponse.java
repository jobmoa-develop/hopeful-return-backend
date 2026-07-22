package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "상담 슬롯 상담사 일괄 배정 결과")
public record BulkCounselorAssignResponse(
        @Schema(description = "처리된 건수", example = "3")
        int updatedCount,

        @Schema(description = "처리된 수강건 ID 목록", example = "[1, 2, 3]")
        List<Long> updatedIds
) {
}
