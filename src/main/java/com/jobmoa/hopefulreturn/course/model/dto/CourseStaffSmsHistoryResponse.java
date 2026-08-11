package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "강좌 담당자 안내 문자 발송 이력")
public record CourseStaffSmsHistoryResponse(
        List<Item> content
) {
    @Schema(description = "발송 이력 항목")
    public record Item(
            @Schema(description = "발송 이력 ID") Long courseStaffSmsId,
            @Schema(description = "수신 담당자 ID") Long userId,
            @Schema(description = "수신 담당자명") String userName,
            @Schema(description = "발송한 계정 ID") Long sentBy,
            @Schema(description = "발송한 계정명") String sentByName,
            @Schema(description = "알림 종류(STATUS_CHANGE / SCHEDULE_CHANGE)") String notifyType,
            @Schema(description = "발송 내용") String content,
            @Schema(description = "발송 상태(SUCCESS / FAIL)") String sendStatus,
            @Schema(description = "발송 시각") LocalDateTime sentAt
    ) {
    }
}