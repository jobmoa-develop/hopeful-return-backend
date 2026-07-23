package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "강좌 목록 응답")
public record CourseListResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    @Schema(description = "강좌 목록 항목")
    public record Item(
            @Schema(description = "강좌 ID", example = "15")
            Long courseId,

            @Schema(description = "강좌명", example = "양천 5기 희망리턴과정")
            String courseName,

            @Schema(description = "기수", example = "5")
            Integer courseNumber,

            @Schema(description = "Local course number", example = "2")
            Integer localCourseNumber,

            @Schema(description = "지역명", example = "양천")
            String regionName,

            @Schema(description = "강좌 상태", example = "PLANNED")
            String status,

            @Schema(description = "정원", example = "40")
            Integer capacity,

            @Schema(description = "현재 참여자 수", example = "28")
            int currentParticipantCount,

            @Schema(description = "교육 연도(day1_date 파생)", example = "2026")
            Integer year,

            @Schema(description = "교육 1일차", example = "2026-08-18")
            LocalDate day1Date,

            @Schema(description = "교육 2일차", example = "2026-08-19")
            LocalDate day2Date,

            @Schema(description = "교육 3일차", example = "2026-08-20")
            LocalDate day3Date,

            @Schema(description = "교육 4일차", example = "2026-08-21")
            LocalDate day4Date,

            @Schema(description = "교육 5일차", example = "2026-08-22")
            LocalDate day5Date,

            @Schema(description = "휴게시간", example = "01:00:00")
            LocalTime breakTime
    ) {
    }
}
