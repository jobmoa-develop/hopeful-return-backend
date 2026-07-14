package com.jobmoa.hopefulreturn.coursedailystaff.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * 배정 저장 시 다른 폐강되지 않은 회차와 겹친 충돌 항목.
 * 저장 요청 인력이 같은 날짜·겹치는 시간대에 다른 회차에 이미 배정되어 있을 때 반환한다.
 */
@Schema(description = "타 회차 중복 배정 충돌 항목")
public record AssignConflict(
        @Schema(description = "충돌 인력 사용자 ID", example = "6")
        Long userId,

        @Schema(description = "충돌 인력 이름", example = "이강사")
        String name,

        @Schema(description = "충돌 날짜", example = "2026-08-18")
        LocalDate scheduleDate,

        @Schema(description = "요청한 시간대(AM/PM/FULL)", example = "AM")
        String sessionType,

        @Schema(description = "이미 배정된 다른 회차 ID", example = "12")
        Long courseId,

        @Schema(description = "이미 배정된 다른 회차명", example = "양천5기")
        String courseName,

        @Schema(description = "다른 회차에서의 역할", example = "LECTURER")
        String staffRole
) {
}
