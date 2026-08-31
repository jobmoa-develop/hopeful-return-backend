package com.jobmoa.hopefulreturn.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.List;

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

        @Schema(description = "직책(근무기록표 인쇄용)", example = "책임")
        @Size(max = 50)
        String position,

        @Schema(description = "역할명 목록", example = "[\"HEAD_OFFICE\"]")
        @NotEmpty
        List<String> roleNames,

        @Schema(description = "활성 여부", example = "true")
        @NotNull
        Boolean enabled,

        @Schema(description = "잠금 여부", example = "false")
        @NotNull
        Boolean locked
) {
}
