package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "상담 슬롯 상담사 일괄 배정 요청 (선택한 수강건들에 동일 슬롯·상담사 적용)")
public record BulkAssignCounselorRequest(
        @Schema(description = "대상 수강건 ID 목록", example = "[1, 2, 3]")
        @NotEmpty
        List<Long> courseParticipantIds,

        @Schema(description = "상담 구분 — PRE_SESSION / POST_SESSION_1 / POST_SESSION_2", example = "PRE_SESSION")
        @NotBlank
        String counselingType,

        @Schema(description = "지정할 상담사(직원) ID", example = "12")
        @NotNull
        Long counselorId
) {
}
