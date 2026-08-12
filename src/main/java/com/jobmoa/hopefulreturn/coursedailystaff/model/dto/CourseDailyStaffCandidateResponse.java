package com.jobmoa.hopefulreturn.coursedailystaff.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * 회차 날짜별 배정 후보 직원. 회차 교육일(day1~day5) 범위에서 가용(is_available=true)한
 * 스태프와, 각 스태프가 채울 수 있는 배정 역할(StaffRole)·가용 시간대를 함께 내려준다.
 */
@Schema(description = "회차 날짜별 배정 후보 직원 응답")
public record CourseDailyStaffCandidateResponse(
        @Schema(description = "회차 ID", example = "15")
        Long courseId,

        @Schema(description = "회차 교육일 목록(day1~day5)", example = "[\"2026-08-18\",\"2026-08-19\"]")
        List<LocalDate> dates,

        List<Candidate> candidates
) {

    @Schema(description = "후보 직원")
    public record Candidate(
            @Schema(description = "사용자 ID", example = "6")
            Long userId,

            @Schema(description = "이름", example = "이강사")
            String name,

            @Schema(description = "전화번호(배정 변경 안내 문자용)", example = "01012345678")
            String phone,

            @Schema(description = "채울 수 있는 배정 역할 목록",
                    example = "[\"LECTURER\"]")
            List<String> staffRoles,

            @Schema(description = "가용 시간대 목록")
            List<Availability> availability,

            @Schema(description = "다른 폐강되지 않은 회차에 이미 배정된 슬롯(중복 방지·경고용). "
                    + "session 겹칠 때만 충돌: FULL은 전부, AM/PM은 동일 세션.")
            List<Busy> busy
    ) {

        @Schema(description = "가용 날짜·시간대")
        public record Availability(
                @Schema(description = "날짜", example = "2026-08-18")
                LocalDate scheduleDate,

                @Schema(description = "시간대(AM/PM/FULL)", example = "AM")
                String sessionType
        ) {
        }

        @Schema(description = "다른 회차 배정 슬롯(폐강 제외)")
        public record Busy(
                @Schema(description = "날짜", example = "2026-08-18")
                LocalDate scheduleDate,

                @Schema(description = "시간대(AM/PM/FULL)", example = "AM")
                String sessionType,

                @Schema(description = "배정된 회차 ID", example = "12")
                Long courseId,

                @Schema(description = "배정된 회차명", example = "양천5기")
                String courseName,

                @Schema(description = "배정된 역할", example = "LECTURER")
                String staffRole
        ) {
        }
    }
}
