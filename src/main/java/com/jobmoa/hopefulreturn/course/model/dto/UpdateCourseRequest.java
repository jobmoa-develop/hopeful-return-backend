package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "강좌 수정 요청")
public record UpdateCourseRequest(
        @Schema(description = "지역 ID", example = "3")
        Long regionId,

        @Schema(description = "기수", example = "5")
        @Positive
        Integer courseNumber,

        @Schema(description = "Local course number", example = "2")
        @Positive
        Integer localCourseNumber,

        @Schema(description = "강좌명", example = "양천 5기 희망리턴 심화과정")
        String courseName,

        @Schema(description = "모집 시작일", example = "2026-08-01")
        LocalDate recruitStart,

        @Schema(description = "모집 종료일", example = "2026-08-10")
        LocalDate recruitEnd,

        @Schema(description = "1일차 교육일", example = "2026-08-17")
        LocalDate day1Date,

        @Schema(description = "2일차 교육일", example = "2026-08-18")
        LocalDate day2Date,

        @Schema(description = "3일차 교육일", example = "2026-08-19")
        LocalDate day3Date,

        @Schema(description = "4일차 교육일", example = "2026-08-20")
        LocalDate day4Date,

        @Schema(description = "5일차 교육일", example = "2026-08-21")
        LocalDate day5Date,

        @Schema(description = "교육 시작 시간", example = "09:00")
        LocalTime educationStartTime,

        @Schema(description = "교육 종료 시간", example = "18:00")
        LocalTime educationEndTime,

        @Schema(description = "정원", example = "45")
        @Positive
        Integer capacity,

        @Schema(description = "최소 정원", example = "20")
        @Positive
        Integer minimumCapacity,

        @Schema(description = "교육 장소", example = "양천센터 제2교육장")
        String location,

        @Schema(description = "계획서 제출일", example = "2026-07-20")
        LocalDate planSubmitDate
) {
}