package com.jobmoa.hopefulreturn.coursedailystaff.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "회차 날짜별 인력 배정 목록 응답")
public record CourseDailyStaffListResponse(
        @Schema(description = "회차 ID", example = "15")
        Long courseId,

        List<Item> assignments
) {

    @Schema(description = "회차 날짜별 인력 배정 항목")
    public record Item(
            @Schema(description = "배정 ID", example = "42")
            Long courseDailyStaffId,

            @Schema(description = "배정 날짜", example = "2026-08-18")
            LocalDate scheduleDate,

            @Schema(description = "배정 역할", example = "LECTURER")
            String staffRole,

            @Schema(description = "시간대", example = "AM")
            String sessionType,

            @Schema(description = "배정 인력 사용자 ID", example = "6")
            Long userId,

            @Schema(description = "배정 인력 이름", example = "이강사")
            String name
    ) {
    }
}
