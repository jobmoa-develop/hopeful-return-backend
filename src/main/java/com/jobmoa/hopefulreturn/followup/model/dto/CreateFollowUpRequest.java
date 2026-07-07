package com.jobmoa.hopefulreturn.followup.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "사후관리 등록 요청")
public record CreateFollowUpRequest(
        @Schema(description = "수강생 ID", example = "15")
        @NotNull
        Long courseParticipantId,

        @Schema(description = "사후관리 개월차", example = "1")
        Integer monthNo,

        @Schema(description = "상담일", example = "2026-09-24")
        LocalDate contactDate,

        @Schema(description = "상담 유형", example = "PHONE")
        String contactType,

        @Schema(description = "취업 상태", example = "LOOKING")
        String employmentStatus,

        @Schema(description = "국민취업지원제도 참여 상태", example = "PARTICIPATING")
        String nationalProgram,

        @Schema(description = "산림복지 프로그램 참여 여부", example = "false")
        Boolean forestProgram,

        @Schema(description = "비고", example = "구직활동 진행 중")
        String remark
) {
}
