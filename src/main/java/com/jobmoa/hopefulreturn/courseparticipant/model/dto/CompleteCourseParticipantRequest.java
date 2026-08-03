package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "수료 처리 요청 (status = COMPLETED 또는 INCOMPLETE)")
public record CompleteCourseParticipantRequest(
        @Schema(description = "수료 상태", example = "COMPLETED")
        @NotBlank
        String status,

        @Schema(description = "수료일", example = "2026-08-24")
        LocalDate completionDate,

        @Schema(description = "미수료 사유", example = "개인 사정")
        @Size(max = 255)
        String incompleteReason
) {
}
