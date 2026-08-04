package com.jobmoa.hopefulreturn.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "내 정보 수정 요청(전화번호/이메일)")
public record UpdateMyProfileRequest(
        @Schema(description = "전화번호", example = "01045871737")
        @Size(max = 20)
        String phone,

        @Schema(description = "이메일", example = "leeic@jobmoa.com")
        @Email
        @Size(max = 100)
        String email
) {
}