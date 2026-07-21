package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 일괄 등록 커밋 결과. 매핑되지 않은/중복/오류 행은 스킵되고 사유가 {@code details} 에 담긴다(부분 등록).
 */
@Schema(description = "일괄 등록 커밋 결과")
public record BulkImportResultResponse(
        @Schema(description = "신규 등록된 수강건 수", example = "250")
        int registeredCount,

        @Schema(description = "이미 같은 회차에 등록돼 스킵된 수", example = "5")
        int skippedDuplicateCount,

        @Schema(description = "회차 미매핑으로 스킵된 수", example = "15")
        int skippedUnmappedCount,

        @Schema(description = "파싱/검증 오류로 제외된 수", example = "3")
        int invalidRowCount,

        @Schema(description = "새로 생성된 참여자 수", example = "240")
        int createdParticipantCount,

        @Schema(description = "기존 참여자로 재사용된 수", example = "10")
        int reusedParticipantCount,

        @Schema(description = "스킵/오류 행 사유 리포트")
        List<BulkImportRowResult> details
) {
}
