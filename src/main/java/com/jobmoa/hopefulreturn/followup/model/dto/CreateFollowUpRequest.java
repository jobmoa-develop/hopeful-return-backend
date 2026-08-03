package com.jobmoa.hopefulreturn.followup.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "사후관리 등록 요청")
public record CreateFollowUpRequest(
        @Schema(description = "수강생 ID", example = "15")
        @NotNull
        Long courseParticipantId,

        @Schema(description = "취업일", example = "2026-09-24")
        LocalDate employmentDate,

        @Schema(description = "숲체험 프로그램 참여일", example = "2026-10-05")
        LocalDate forestProgramDate,

        @Schema(description = "국민취업지원제도 연계일", example = "2026-10-20")
        LocalDate nationalProgramDate,

        @Schema(description = "국민취업지원제도 지점", example = "남부")
        String nationalProgramBranch
) {
}
