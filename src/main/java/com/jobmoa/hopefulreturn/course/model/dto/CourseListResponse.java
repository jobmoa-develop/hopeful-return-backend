package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

            @Schema(description = "지역명", example = "양천")
            String regionName,

            @Schema(description = "강좌 상태", example = "PLANNED")
            String status,

            @Schema(description = "정원", example = "40")
            Integer capacity,

            @Schema(description = "현재 참여자 수", example = "28")
            int currentParticipantCount
    ) {
    }
}
