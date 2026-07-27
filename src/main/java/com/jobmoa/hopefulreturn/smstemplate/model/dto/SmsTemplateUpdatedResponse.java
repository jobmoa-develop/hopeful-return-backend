package com.jobmoa.hopefulreturn.smstemplate.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문자 템플릿 수정 응답")
public record SmsTemplateUpdatedResponse(
        @Schema(description = "문자 템플릿 ID", example = "12")
        Long smsTemplateId,

        @Schema(description = "수정 여부", example = "true")
        boolean updated
) {
}
