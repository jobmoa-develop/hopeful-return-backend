package com.jobmoa.hopefulreturn.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "Access Token", example = "")
        String accessToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 만료 시간(초)", example = "3600")
        Long expiresIn,

        @Schema(description = "사용자 정보")
        User user
) {

    @Schema(description = "로그인 사용자 정보")
    public record User(
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

            @Schema(description = "역할 목록", example = "[\"HEAD_OFFICE\", \"COUNSELOR\"]")
            List<String> roles,

            @Schema(description = "문자 발송 권한 보유 여부", example = "false")
            Boolean canSendSms
    ) {
    }
}