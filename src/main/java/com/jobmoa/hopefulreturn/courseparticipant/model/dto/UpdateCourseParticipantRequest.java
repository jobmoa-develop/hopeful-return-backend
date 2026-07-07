package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "수강 정보 수정 요청 (부분 수정 — null 필드는 미변경)")
public record UpdateCourseParticipantRequest(
        @Schema(description = "상담사(사용자) ID", example = "10")
        Long counselorId,

        @Schema(description = "기초교육 이수 여부", example = "N")
        @Size(max = 20)
        String basicEducation,

        @Schema(description = "유입 경로", example = "지인추천")
        @Size(max = 30)
        String inflowType
) {
}
