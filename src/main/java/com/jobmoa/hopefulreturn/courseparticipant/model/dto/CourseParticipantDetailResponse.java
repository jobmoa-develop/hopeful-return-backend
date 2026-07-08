package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "수강생 상세 응답")
public record CourseParticipantDetailResponse(
        @Schema(description = "수강 정보 ID", example = "101")
        Long courseParticipantId,

        @Schema(description = "참여자 ID", example = "25")
        Long participantId,

        @Schema(description = "참여자명", example = "김철수")
        String participantName,

        @Schema(description = "강좌 ID", example = "15")
        Long courseId,

        @Schema(description = "강좌명", example = "양천5기")
        String courseName,

        @Schema(description = "상담사 배정 목록 (상담사 + 사전/사후 구분)")
        List<CounselorSummary> counselors,

        @Schema(description = "수강 상태", example = "APPLIED")
        String status,

        @Schema(description = "연락 시도 횟수", example = "0")
        Integer contactAttempt,

        @Schema(description = "기초교육 이수 여부", example = "Y")
        String basicEducation
) {
}
