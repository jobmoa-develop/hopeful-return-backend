package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "단일 상담 슬롯의 상담사 지정 요청")
public record AssignSlotCounselorRequest(
        @Schema(description = "지정할 상담사(직원) ID", example = "12")
        @NotNull
        Long counselorId
) {
}
