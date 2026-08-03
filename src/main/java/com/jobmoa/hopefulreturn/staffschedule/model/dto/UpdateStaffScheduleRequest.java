package com.jobmoa.hopefulreturn.staffschedule.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 스태프 일정 수정 요청. 날짜/사용자 변경은 지원하지 않으며(삭제 후 재등록 권장),
 * sessionType·isAvailable·memo 만 수정한다. 각 필드는 null 이면 미변경으로 취급한다.
 */
@Schema(description = "스태프 일정 수정 요청")
public record UpdateStaffScheduleRequest(
        @Schema(description = "시간대(AM/PM/FULL). null이면 미변경", example = "PM")
        String sessionType,

        @Schema(description = "가용 여부. null이면 미변경", example = "false")
        Boolean isAvailable,

        @Schema(description = "비고(사유 등). null이면 미변경", example = "오후로 변경")
        String memo
) {
}
