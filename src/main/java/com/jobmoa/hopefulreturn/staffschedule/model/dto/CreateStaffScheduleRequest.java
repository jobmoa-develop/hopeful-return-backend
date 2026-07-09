package com.jobmoa.hopefulreturn.staffschedule.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "스태프 일정 단건 등록 요청")
public record CreateStaffScheduleRequest(
        @Schema(description = "등록 대상 스태프 ID(생략 시 인증 사용자 본인). 타인 지정은 ADMIN·OPERATOR만 가능", example = "6")
        Long userId,

        @Schema(description = "참여 가능 날짜", example = "2026-08-18")
        @NotNull
        LocalDate scheduleDate,

        @Schema(description = "시간대(AM/PM/FULL)", example = "AM")
        @NotBlank
        String sessionType,

        @Schema(description = "가용 여부(생략 시 true)", example = "true")
        Boolean isAvailable,

        @Schema(description = "비고(사유 등)", example = "오전만 가능")
        String memo
) {
}
