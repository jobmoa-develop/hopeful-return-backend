package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

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

        @Schema(description = "상담사(사용자) ID", example = "8")
        Long counselorId,

        @Schema(description = "상담사명", example = "홍길동")
        String counselorName,

        @Schema(description = "수강 상태", example = "APPLIED")
        String status,

        @Schema(description = "연락 시도 횟수", example = "0")
        Integer contactAttempt,

        @Schema(description = "기초교육 이수 여부", example = "Y")
        String basicEducation
) {
}
