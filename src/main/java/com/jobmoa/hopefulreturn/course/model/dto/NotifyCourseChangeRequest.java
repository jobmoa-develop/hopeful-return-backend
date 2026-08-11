package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "강좌 일정/장소 변경 안내 문자 발송 요청")
public record NotifyCourseChangeRequest(
        @Schema(description = "발송 대상 담당자(user) ID 목록 — 이 회차에 실제 배치된 PM 이외 담당자만 유효",
                example = "[3, 5]")
        @NotEmpty
        List<Long> userIds
) {
}