package com.jobmoa.hopefulreturn.followup.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "사후관리 상세 응답")
public record FollowUpDetailResponse(
        @Schema(description = "사후관리 ID", example = "11")
        Long followUpId,

        @Schema(description = "수강생 ID", example = "15")
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
