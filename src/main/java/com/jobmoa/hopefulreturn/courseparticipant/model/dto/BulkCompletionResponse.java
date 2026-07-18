package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "일괄 수료 처리 결과")
public record BulkCompletionResponse(
        @Schema(description = "처리된 건수", example = "3")
        int updatedCount,

        @Schema(description = "처리된 수강건 ID 목록", example = "[1, 2, 3]")
        List<Long> updatedIds
) {
}
