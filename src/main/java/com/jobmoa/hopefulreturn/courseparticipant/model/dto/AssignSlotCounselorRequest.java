package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import com.jobmoa.hopefulreturn.courseparticipant.entity.ChangeSubject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "단일 상담 슬롯의 상담사 지정 요청")
public record AssignSlotCounselorRequest(
        @Schema(description = "지정할 상담사(직원) ID", example = "12")
        @NotNull
        Long counselorId,

        @Schema(description = "변경 주체 — NONE(빈칸) / COUNSELOR(상담사) / PARTICIPANT(참여자)", example = "COUNSELOR")
        @NotNull
        ChangeSubject changedBy,

        @Schema(description = "변경 비고(필수)", example = "상담사 사정으로 사후1 상담사 교체")
        @NotBlank
        String reason
) {
}
