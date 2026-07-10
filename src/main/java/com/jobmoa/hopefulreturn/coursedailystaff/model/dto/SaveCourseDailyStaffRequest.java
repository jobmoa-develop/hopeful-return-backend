package com.jobmoa.hopefulreturn.coursedailystaff.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 회차 날짜별 인력 배정 저장(그리드 단위 upsert). 해당 회차의 기존 배정을 전량 교체한다.
 */
@Schema(description = "회차 날짜별 인력 배정 저장 요청")
public record SaveCourseDailyStaffRequest(
        @Schema(description = "회차 ID", example = "15")
        @NotNull
        Long courseId,

        @Schema(description = "배정 항목 목록(그리드 전체)")
        @NotNull
        @Valid
        List<Entry> entries
) {

    @Schema(description = "배정 항목")
    public record Entry(
            @Schema(description = "배정 날짜", example = "2026-08-18")
            @NotNull
            LocalDate scheduleDate,

            @Schema(description = "배정 역할", example = "LECTURER")
            @NotBlank
            String staffRole,

            @Schema(description = "시간대(AM/PM/FULL)", example = "AM")
            @NotBlank
            String sessionType,

            @Schema(description = "배정 인력 사용자 ID", example = "6")
            @NotNull
            Long userId
    ) {
    }
}
