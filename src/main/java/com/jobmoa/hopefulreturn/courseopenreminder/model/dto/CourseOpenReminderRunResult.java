package com.jobmoa.hopefulreturn.courseopenreminder.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "개강 하루전 자동 문자 배치 실행 결과")
public record CourseOpenReminderRunResult(
        @Schema(description = "대상 회차 수", example = "3")
        int targetCourses,

        @Schema(description = "발송 성공 건수", example = "7")
        int sent,

        @Schema(description = "발송 실패 건수", example = "0")
        int failed,

        @Schema(description = "건너뛴 건수(전화번호 없음·이미 발송)", example = "1")
        int skipped
) {
}
