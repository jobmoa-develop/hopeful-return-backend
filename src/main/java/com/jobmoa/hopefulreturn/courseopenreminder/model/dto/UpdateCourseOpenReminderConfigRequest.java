package com.jobmoa.hopefulreturn.courseopenreminder.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Schema(description = "개강 하루전 자동 문자 발송 설정 수정 요청")
public record UpdateCourseOpenReminderConfigRequest(
        @Schema(description = "자동 발송 활성화 여부", example = "true")
        @NotNull Boolean enabled,

        @Schema(description = "개강 며칠 전 발송(1~30)", example = "1")
        @NotNull @Min(1) @Max(30) Integer daysBefore,

        @Schema(description = "발송 시각(HH:mm)", example = "09:00")
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime sendTime
) {
}
