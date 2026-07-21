package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 파일 안에서 같은 교육과정명(정부식 회차)으로 묶인 참여자 그룹. 운영자는 그룹별로 내부 회차를 매핑한다.
 */
@Schema(description = "교육과정명(정부식 회차)별 참여자 그룹")
public record BulkImportCourseGroup(
        @Schema(description = "교육과정명(매핑 키)", example = "[현장] (서울)리본(Re:Born)커리어_16회차")
        String sourceCourseName,

        @Schema(description = "시도", example = "서울특별시")
        String sido,

        @Schema(description = "시군구", example = "서울특별시 양천구")
        String sigungu,

        @Schema(description = "교육과정명에서 추출한 회차 번호(추출 실패 시 null)", example = "16")
        Integer roundNumber,

        @Schema(description = "그룹 내 총 인원(오류 행 포함)", example = "12")
        int participantCount,

        @Schema(description = "그룹 내 파싱/검증 오류 행 수", example = "0")
        int invalidCount,

        @Schema(description = "자동 추천 내부 회차 courseId(대개 null — 지역·번호 체계 불일치)", example = "1")
        Long suggestedCourseId,

        @Schema(description = "그룹 내 파싱 행 목록")
        List<BulkImportParsedRow> rows
) {
}
