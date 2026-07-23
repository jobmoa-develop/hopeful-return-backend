package com.jobmoa.hopefulreturn.followup.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "사후관리 수정 요청")
public record UpdateFollowUpRequest(
        @Schema(description = "취업일", example = "2026-09-25")
        LocalDate employmentDate,

        @Schema(description = "숲체험 프로그램 참여일", example = "2026-10-06")
        LocalDate forestProgramDate,

        @Schema(description = "국민취업지원제도 연계일", example = "2026-10-21")
        LocalDate nationalProgramDate,

        @Schema(description = "국민취업지원제도 지점", example = "관악")
        String nationalProgramBranch
) {
}
