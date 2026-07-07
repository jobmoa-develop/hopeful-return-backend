package com.jobmoa.hopefulreturn.attendance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일차별 출석 일괄 등록 응답")
public record BulkAttendanceResponse(
        @Schema(description = "저장된 출석 건수", example = "3")
        int savedCount,

        @Schema(description = "수업 차수(일차)", example = "1")
        Integer dayNo,

        @Schema(description = "강좌 ID", example = "10")
        Long courseId,

        @Schema(description = "결과 메시지", example = "출석 정보가 저장되었습니다.")
        String message
) {
}
