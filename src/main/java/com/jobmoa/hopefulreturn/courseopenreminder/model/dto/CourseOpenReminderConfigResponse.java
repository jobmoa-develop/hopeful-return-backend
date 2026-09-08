package com.jobmoa.hopefulreturn.courseopenreminder.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobmoa.hopefulreturn.courseopenreminder.entity.CourseOpenReminderConfigEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "개강 하루전 자동 문자 발송 설정 응답")
public record CourseOpenReminderConfigResponse(
        @Schema(description = "자동 발송 활성화 여부", example = "true")
        boolean enabled,

        @Schema(description = "개강 며칠 전 발송(1=하루 전)", example = "1")
        int daysBefore,

        @Schema(description = "발송 시각(HH:mm)", example = "09:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime sendTime,

        @Schema(description = "마지막 배치 실행일(하루 1회 가드)", example = "2026-09-08")
        LocalDate lastRunDate,

        @Schema(description = "설정 수정 시각")
        LocalDateTime updatedAt,

        @Schema(description = "설정 수정자 사용자 ID", example = "1")
        Long updatedBy
) {

    public static CourseOpenReminderConfigResponse from(CourseOpenReminderConfigEntity e) {
        return new CourseOpenReminderConfigResponse(
                e.isEnabled(), e.getDaysBefore(), e.getSendTime(),
                e.getLastRunDate(), e.getUpdatedAt(), e.getUpdatedBy());
    }
}
