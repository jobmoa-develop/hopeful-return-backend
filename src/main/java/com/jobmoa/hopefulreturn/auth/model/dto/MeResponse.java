package com.jobmoa.hopefulreturn.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 정보 응답")
public record MeResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "로그인 ID", example = "headoffice01")
        String loginId,

        @Schema(description = "이름", example = "이인철")
        String name,

        @Schema(description = "전화번호", example = "01045871737")
        String phone,

        @Schema(description = "이메일", example = "leeic@jobmoa.com")
        String email,

        @Schema(description = "역할명", example = "HEAD_OFFICE")
        String roleName
) {
}
