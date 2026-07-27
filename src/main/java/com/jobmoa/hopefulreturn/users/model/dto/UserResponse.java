package com.jobmoa.hopefulreturn.users.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "직원 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        @Schema(description = "직원 ID", example = "2")
        Long userId,

        @Schema(description = "로그인 ID", example = "operator01")
        String loginId,

        @Schema(description = "직원명", example = "한준희")
        String name,

        @Schema(description = "전화번호", example = "0215665011")
        String phone,

        @Schema(description = "이메일", example = "hanjh@jobmoa.com")
        String email,

        @Schema(description = "역할명 목록", example = "[\"OPERATOR\"]")
        List<String> roleNames,

        @Schema(description = "활성 여부", example = "true")
        Boolean enabled,

        @Schema(description = "잠금 여부", example = "false")
        Boolean locked,

        @Schema(description = "생성 일시", example = "2026-07-02T09:00:00")
        LocalDateTime createdAt,

        @Schema(description = "문자 발송 권한 보유 여부", example = "false")
        Boolean canSendSms
) {
}
