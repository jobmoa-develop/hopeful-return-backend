package com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "시스템 문자 템플릿 수정 요청(본문만 수정)")
public record UpdateSystemMessageTemplateRequest(
        @Schema(description = "발송 본문(치환 토큰 포함)", example = "[인증코드] {code}")
        @NotBlank
        @Size(max = 2000)
        String content
) {
}
