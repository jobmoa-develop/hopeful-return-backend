package com.jobmoa.hopefulreturn.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access Token 재발급 응답")
public record RefreshResponse(
        @Schema(description = "Access Token", example = "")
        String accessToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 만료 시간(초)", example = "3600")
        Long expiresIn
) {
}
