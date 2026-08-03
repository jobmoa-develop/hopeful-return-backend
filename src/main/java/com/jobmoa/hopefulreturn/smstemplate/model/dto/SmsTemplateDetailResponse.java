package com.jobmoa.hopefulreturn.smstemplate.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "문자 템플릿 상세 응답")
public record SmsTemplateDetailResponse(
        @Schema(description = "문자 템플릿 ID", example = "12")
        Long smsTemplateId,

        @Schema(description = "공개 범위", example = "PERSONAL")
        String scope,

        @Schema(description = "소유 계정 ID(공용은 null)", example = "4")
        Long userId,

        @Schema(description = "템플릿 제목", example = "수료 안내")
        String title,

        @Schema(description = "템플릿 내용", example = "{name}님, 수료를 축하합니다.")
        String content,

        @Schema(description = "등록일시", example = "2026-07-24T15:20:10")
        LocalDateTime createdAt,

        @Schema(description = "수정일시", example = "2026-07-24T15:20:10")
        LocalDateTime updatedAt
) {
}
