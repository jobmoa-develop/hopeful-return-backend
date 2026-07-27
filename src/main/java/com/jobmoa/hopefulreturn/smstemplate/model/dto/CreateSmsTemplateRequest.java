package com.jobmoa.hopefulreturn.smstemplate.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "문자 템플릿 등록 요청")
public record CreateSmsTemplateRequest(
        @Schema(description = "공개 범위 (PERSONAL=개인, SHARED=공용)", example = "PERSONAL")
        @NotBlank
        String scope,

        @Schema(description = "템플릿 제목", example = "수료 안내")
        @Size(max = 100)
        String title,

        @Schema(description = "템플릿 내용 ({name} = 성명 치환)", example = "{name}님, 수료를 축하합니다.")
        @NotBlank
        @Size(max = 2000)
        String content
) {
}
