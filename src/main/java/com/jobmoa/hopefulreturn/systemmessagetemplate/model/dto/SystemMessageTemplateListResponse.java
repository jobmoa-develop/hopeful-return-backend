package com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "시스템 문자 템플릿 목록 응답")
public record SystemMessageTemplateListResponse(
        List<Item> content
) {

    @Schema(description = "시스템 문자 템플릿 목록 항목")
    public record Item(
            @Schema(description = "시스템 문자 템플릿 ID", example = "3")
            Long systemMessageTemplateId,

            @Schema(description = "고정 key", example = "COURSE_OPEN_REMINDER")
            String templateKey,

            @Schema(description = "표시명", example = "개강 하루전 안내")
            String name,

            @Schema(description = "용도·치환 토큰 안내")
            String description,

            @Schema(description = "발송 본문(치환 토큰 포함)")
            String content,

            @Schema(description = "수정일시", example = "2026-09-08T15:20:10")
            LocalDateTime updatedAt
    ) {
    }
}
