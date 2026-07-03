package com.jobmoa.hopefulreturn.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 ID 중복 확인 응답")
public record CheckLoginIdResponse(
        @Schema(description = "중복 여부", example = "false")
        boolean duplicate
) {
}
