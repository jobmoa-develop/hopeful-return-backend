package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "수강 등록 요청")
public record CreateCourseParticipantRequest(
        @Schema(description = "강좌 ID", example = "15")
        @NotNull
        Long courseId,

        @Schema(description = "참여자 ID", example = "25")
        @NotNull
        Long participantId,

        @Schema(description = "상담사 배정 목록 (상담사 + 사전/사후 구분)")
        @Valid
        List<CounselorAssignment> counselors,

        @Schema(description = "유입 경로", example = "워크넷")
        @Size(max = 30)
        String inflowType,

        @Schema(description = "신청일", example = "2026-08-01")
        LocalDate applyDate,

        @Schema(description = "접수일", example = "2026-08-02")
        LocalDate receptionDate,

        @Schema(description = "기초교육 이수 여부", example = "Y")
        @Size(max = 20)
        String basicEducation
) {
}
