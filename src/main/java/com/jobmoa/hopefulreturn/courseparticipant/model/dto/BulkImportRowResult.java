package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 커밋 시 등록에서 제외(스킵/오류)된 행의 사유 리포트. 정상 등록된 행은 details 에 포함하지 않는다.
 */
@Schema(description = "일괄 등록 처리 행 결과(스킵/오류 리포트)")
public record BulkImportRowResult(
        @Schema(description = "엑셀 데이터 행 번호(1-based)", example = "5")
        int rowNumber,

        @Schema(description = "교육생명", example = "홍길동")
        String name,

        @Schema(description = "교육과정명", example = "[현장] (인천)리본(Re:Born)커리어_23회차")
        String sourceCourseName,

        @Schema(description = "처리 결과 — SKIPPED_UNMAPPED / SKIPPED_DUPLICATE / INVALID", example = "SKIPPED_UNMAPPED")
        String outcome,

        @Schema(description = "사유", example = "매핑된 회차가 없습니다.")
        String reason
) {
}
