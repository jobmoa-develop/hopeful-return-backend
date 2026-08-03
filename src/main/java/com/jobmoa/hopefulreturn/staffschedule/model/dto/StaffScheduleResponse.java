package com.jobmoa.hopefulreturn.staffschedule.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "스태프 일정 응답(단건/상세)")
public record StaffScheduleResponse(
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

        @Schema(description = "비고", example = "오전만 가능")
        String memo,

        @Schema(description = "생성 시각", example = "2026-07-08T14:20:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각", example = "2026-07-08T14:20:00")
        LocalDateTime updatedAt
) {
}
