package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "강좌 수정 요청")
public record UpdateCourseRequest(
        @Schema(description = "강좌명", example = "양천 5기 희망리턴 심화과정")
        String courseName,

        @Schema(description = "정원", example = "45")
        @Positive
        Integer capacity,

        @Schema(description = "최소 정원", example = "20")
        @Positive
        Integer minimumCapacity,

        @Schema(description = "교육 장소", example = "양천센터 제2교육장")
        String location
) {
}
