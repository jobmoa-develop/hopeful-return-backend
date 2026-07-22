package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "수강 정보 수정 요청 (부분 수정 — null 필드는 미변경)")
public record UpdateCourseParticipantRequest(
        @Schema(description = "상담사 배정 목록 (제공 시 전체 교체, null이면 미변경)")
        @Valid
        List<CounselorAssignment> counselors,

        @Schema(description = "기초교육 이수 여부", example = "N")
        @Size(max = 20)
        String basicEducation,

        @Schema(description = "유입 경로", example = "지인추천")
        @Size(max = 30)
        String inflowType,

        @Schema(description = "신청일 (null이면 미변경)", example = "2026-08-01")
        LocalDate applyDate,

        @Schema(description = "접수일 (null이면 미변경)", example = "2026-08-02")
        LocalDate receptionDate,

        @Schema(description = "연락 시도 횟수 (null이면 미변경)", example = "3")
        Integer contactAttempt
) {
}
