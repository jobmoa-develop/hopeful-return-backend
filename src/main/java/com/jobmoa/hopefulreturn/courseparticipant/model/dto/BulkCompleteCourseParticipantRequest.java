package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "일괄 수료 처리 요청 (선택한 수강건들에 동일 상태·수료일·미수료 사유 적용)")
public record BulkCompleteCourseParticipantRequest(
        @Schema(description = "대상 수강건 ID 목록", example = "[1, 2, 3]")
        @NotEmpty
        List<Long> courseParticipantIds,

        @Schema(description = "수료 상태 (COMPLETED 또는 INCOMPLETE)", example = "COMPLETED")
        @NotBlank
        String status,

        @Schema(description = "수료일 (status=COMPLETED 시)", example = "2026-08-24")
        LocalDate completionDate,

        @Schema(description = "미수료 사유 (status=INCOMPLETE 시)", example = "출석 기준 미달")
        @Size(max = 255)
        String incompleteReason
) {
}
