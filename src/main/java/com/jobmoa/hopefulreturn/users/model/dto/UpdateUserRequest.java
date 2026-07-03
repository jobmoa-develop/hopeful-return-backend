package com.jobmoa.hopefulreturn.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "직원 수정 요청")
public record UpdateUserRequest(
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

        @Schema(description = "역할명", example = "HEAD_OFFICE")
        @NotBlank
        String roleName,

        @Schema(description = "활성 여부", example = "true")
        @NotNull
        Boolean enabled,

        @Schema(description = "잠금 여부", example = "false")
        @NotNull
        Boolean locked
) {
}
