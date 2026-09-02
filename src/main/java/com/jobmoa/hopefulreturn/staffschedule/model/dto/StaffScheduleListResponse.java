package com.jobmoa.hopefulreturn.staffschedule.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "스태프 일정 목록 응답")
public record StaffScheduleListResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    @Schema(description = "스태프 일정 목록 항목")
    public record Item(
            @Schema(description = "스태프 일정 ID", example = "12")
            Long staffScheduleId,

            @Schema(description = "스태프 ID", example = "6")
            Long userId,

            @Schema(description = "스태프명", example = "홍길동")
            String userName,

            @Schema(description = "참여 가능 날짜", example = "2026-08-18")
            LocalDate scheduleDate,

            @Schema(description = "시간대(AM/PM/FULL)", example = "AM")
            String sessionType,

            @Schema(description = "가용 여부", example = "true")
            Boolean isAvailable,

            @Schema(description = "배정 연결 ID(course_staff). null이면 미배정 일정", example = "77")
            Long courseStaffId,

            @Schema(description = "배정된 회차 ID(course). 미배정이면 null", example = "18")
            Long courseId,

            @Schema(description = "배정된 회차명(지역+회차). 미배정이면 null", example = "서울 3회차")
            String courseName,

            @Schema(description = "배정된 회차 상태(CANCELED/OPEN/IN_PROGRESS 등). 미배정이면 null", example = "IN_PROGRESS")
            String courseStatus,

            @Schema(description = "배정 역할(course_staff.staff_role enum명: LECTURER/COUNSELOR/STAFF/"
                    + "PROJECT_MANAGER/PROJECT_LEADER/ADMIN_STAFF). 미배정이면 null", example = "LECTURER")
            String courseStaffRole,

            @Schema(description = "비고", example = "오전만 가능")
            String memo
    ) {
    }
}
