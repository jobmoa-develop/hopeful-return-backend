package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 일괄 등록 미리보기 결과(쓰기 없음). 운영자는 이 응답의 그룹별로 내부 회차를 매핑한 뒤 커밋한다.
 */
@Schema(description = "일괄 등록 미리보기 결과")
public record BulkImportPreviewResponse(
        @Schema(description = "총 데이터 행 수", example = "273")
        int totalRows,

        @Schema(description = "정상 파싱 행 수", example = "270")
        int validRows,

        @Schema(description = "파싱/검증 오류 행 수", example = "3")
        int invalidRows,

        @Schema(description = "교육과정명별 그룹 목록")
        List<BulkImportCourseGroup> groups
) {
}
