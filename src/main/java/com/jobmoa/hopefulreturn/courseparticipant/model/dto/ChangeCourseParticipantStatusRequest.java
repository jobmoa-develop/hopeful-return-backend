package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "진행상태 변경 요청 (status = APPLIED/CONFIRMED/CANCELED/COMPLETED/INCOMPLETE)")
public record ChangeCourseParticipantStatusRequest(
        @Schema(description = "변경할 진행상태", example = "CONFIRMED")
        @NotBlank
        String status
) {
}
