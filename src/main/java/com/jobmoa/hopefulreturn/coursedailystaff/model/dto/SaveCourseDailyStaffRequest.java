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
        List<Entry> entries,

        @Schema(description = "타 회차 중복 배정을 확인하고 진행할지 여부(기본 false). "
                + "false + 충돌 시 409(ASSIGN_CONFLICT)로 충돌 목록 반환, true면 현재 회차로 이동 저장.",
                example = "false")
        Boolean confirmConflicts
) {

    /** 기존 호출부(confirmConflicts 미지정) 호환용 — 기본 false. */
    public SaveCourseDailyStaffRequest(Long courseId, List<Entry> entries) {
        this(courseId, entries, Boolean.FALSE);
    }

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
