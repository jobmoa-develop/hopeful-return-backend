package com.jobmoa.hopefulreturn.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 요청")
public record LoginRequest(
        @Schema(description = "로그인 ID", example = "headoffice01")
        @NotBlank
        @Size(max = 100)
        String loginId,

        @Schema(description = "비밀번호", example = "1234")
        @NotBlank
        @Size(max = 100)
        String password
) {
}
