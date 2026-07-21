package com.jobmoa.hopefulreturn.coursedailystaff.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * 배정 저장 시 대상 인력이 해당 날짜·시간대에 근무 불가일(staff_schedule course_staff_id NULL·
 * is_available=false)로 등록되어 있어 배정이 거부된 항목. FE 는 이 목록으로 어떤 인력·날짜가
 * 불가인지 안내한다(중복 충돌과 달리 override 없는 하드 블록).
 */
@Schema(description = "근무 불가일 배정 거부 항목")
public record AssignUnavailable(
        @Schema(description = "불가 인력 사용자 ID", example = "6")
        Long userId,

        @Schema(description = "불가 인력 이름", example = "이강사")
        String name,

        @Schema(description = "불가 날짜", example = "2026-08-18")
        LocalDate scheduleDate,

        @Schema(description = "요청한 시간대(AM/PM/FULL)", example = "AM")
        String sessionType
) {
}
