package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

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

        @Schema(description = "강좌명", example = "양천 5기 희망리턴과정")
        String courseName,

        @Schema(description = "강좌 상태", example = "PLANNED")
        String status,

        @Schema(description = "정원", example = "40")
        Integer capacity,

        @Schema(description = "최소 정원", example = "15")
        Integer minimumCapacity,

        @Schema(description = "교육 장소", example = "양천센터 제1교육장")
        String location,

        @Schema(description = "계획서 제출일", example = "2026-07-20")
        LocalDate planSubmitDate
) {
}
