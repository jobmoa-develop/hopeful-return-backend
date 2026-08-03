package com.jobmoa.hopefulreturn.smstemplate.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문자 템플릿 삭제 응답")
public record SmsTemplateDeletedResponse(
        @Schema(description = "결과 메시지", example = "문자 템플릿이 삭제되었습니다.")
        String message
) {
}
