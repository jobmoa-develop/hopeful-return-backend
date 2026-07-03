package com.jobmoa.hopefulreturn.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "직원 등록 요청")
public record CreateUserRequest(
        @Schema(description = "로그인 ID", example = "operator01")
        @NotBlank
        @Size(max = 100)
        String loginId,

        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Schema(description = "직원명", example = "한준희")
        @NotBlank
        @Size(max = 50)
        String name,

        @Schema(description = "전화번호", example = "0215665011")
        @Size(max = 20)
        String phone,

        @Schema(description = "이메일", example = "hanjh@jobmoa.com")
        @Email
        @Size(max = 100)
        String email,

        @Schema(description = "역할명", example = "OPERATOR")
        @NotBlank
        String roleName
) {
}
