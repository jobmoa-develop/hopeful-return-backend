package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "수강생 목록 응답")
public record CourseParticipantListResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    @Schema(description = "수강생 목록 항목")
    public record Item(
            @Schema(description = "수강 정보 ID", example = "101")
            Long courseParticipantId,

            @Schema(description = "참여자명", example = "김철수")
            String participantName,

            @Schema(description = "표시용 식별키(matchKey)", example = "KCS_1978_1234")
            String matchKey,

            @Schema(description = "전화번호", example = "010-5678-1234")
            String phone,

            @Schema(description = "지역명", example = "서울")
            String regionName,

            @Schema(description = "강좌명", example = "양천5기")
            String courseName,

            @Schema(description = "회차(course_number)", example = "5")
            Integer courseNumber,

            @Schema(description = "지역 내 회차(local_course_number)", example = "2")
            Integer localCourseNumber,

            @Schema(description = "수강 상태", example = "APPLIED")
            String status,

            @Schema(description = "상담사 배정 목록 (상담사 + 사전/사후 구분)")
            List<CounselorSummary> counselors
    ) {
    }
}
