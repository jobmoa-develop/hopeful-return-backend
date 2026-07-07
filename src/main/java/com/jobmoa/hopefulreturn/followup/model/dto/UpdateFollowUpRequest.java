package com.jobmoa.hopefulreturn.followup.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "사후관리 수정 요청")
public record UpdateFollowUpRequest(
        @Schema(description = "상담일", example = "2026-09-25")
        LocalDate contactDate,

        @Schema(description = "상담 유형", example = "VISIT")
        String contactType,

        @Schema(description = "취업 상태", example = "EMPLOYED")
        String employmentStatus,

        @Schema(description = "국민취업지원제도 참여 상태", example = "COMPLETED")
        String nationalProgram,

        @Schema(description = "산림복지 프로그램 참여 여부", example = "true")
        Boolean forestProgram,

        @Schema(description = "비고", example = "취업 성공")
        String remark
) {
}
