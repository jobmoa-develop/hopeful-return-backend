package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "강좌 상세 응답")
public record CourseDetailResponse(
        @Schema(description = "강좌 ID", example = "15")
        Long courseId,

        @Schema(description = "지역 ID", example = "3")
        Long regionId,

        @Schema(description = "지역명", example = "양천")
        String regionName,

        @Schema(description = "기수", example = "5")
        Integer courseNumber,

        @Schema(description = "Local course number", example = "2")
        Integer localCourseNumber,

        @Schema(description = "강좌명", example = "양천 5기 희망리턴과정")
        String courseName,

        @Schema(description = "강좌 상태", example = "PLANNED")
        String status,

        @Schema(description = "정원", example = "40")
        Integer capacity,

        @Schema(description = "최소 정원", example = "15")
        Integer minimumCapacity,

        @Schema(description = "현재 참여자 수", example = "28")
        Integer currentParticipants,

        @Schema(description = "교육 장소", example = "양천센터 제1교육장")
        String location,

        @Schema(description = "계획서 제출일", example = "2026-07-20")
        LocalDate planSubmitDate,

        // --- 추가된 필드 세트 ---
        @Schema(description = "모집 시작일", example = "2026-07-01")
        LocalDate recruitStart,

        @Schema(description = "모집 종료일", example = "2026-07-10")
        LocalDate recruitEnd,

        @Schema(description = "1일차 교육일", example = "2026-07-15")
        LocalDate day1Date,

        @Schema(description = "2일차 교육일", example = "2026-07-16")
        LocalDate day2Date,

        @Schema(description = "3일차 교육일", example = "2026-07-17")
        LocalDate day3Date,

        @Schema(description = "4일차 교육일", example = "2026-07-18")
        LocalDate day4Date,

        @Schema(description = "5일차 교육일", example = "2026-07-19")
        LocalDate day5Date,

        @Schema(description = "교육 시작 시간", example = "09:00:00")
        LocalTime educationStartTime,

        @Schema(description = "교육 종료 시간", example = "18:00:00")
        LocalTime educationEndTime
) {
}