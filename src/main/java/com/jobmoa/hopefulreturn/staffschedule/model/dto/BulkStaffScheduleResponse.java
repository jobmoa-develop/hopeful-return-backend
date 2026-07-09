package com.jobmoa.hopefulreturn.staffschedule.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스태프 일정 일괄 등록 응답")
public record BulkStaffScheduleResponse(
        @Schema(description = "신규 등록된 건수", example = "3")
        int registered,

        @Schema(description = "UNIQUE 충돌로 스킵된 건수", example = "1")
        int skipped,

        @Schema(description = "결과 메시지", example = "3건 등록, 1건 스킵되었습니다.")
        String message
) {
}
