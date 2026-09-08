package com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시스템 문자 템플릿 수정 응답")
public record SystemMessageTemplateUpdatedResponse(
        @Schema(description = "시스템 문자 템플릿 ID", example = "3")
        Long systemMessageTemplateId,

        @Schema(description = "수정 여부", example = "true")
        boolean updated
) {
}
