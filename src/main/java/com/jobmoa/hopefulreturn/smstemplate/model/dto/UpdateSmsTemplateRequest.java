package com.jobmoa.hopefulreturn.smstemplate.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "문자 템플릿 수정 요청")
public record UpdateSmsTemplateRequest(
        @Schema(description = "템플릿 제목", example = "수료 안내(수정)")
        @Size(max = 100)
        String title,

        @Schema(description = "템플릿 내용 ({name} = 성명 치환)", example = "{name}님, 수료를 진심으로 축하드립니다.")
        @NotBlank
        @Size(max = 2000)
        String content
) {
}
