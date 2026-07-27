package com.jobmoa.hopefulreturn.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "직원 목록 응답")
public record UserListResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    @Schema(description = "직원 목록 항목")
    public record Item(
            @Schema(description = "직원 ID", example = "1")
            Long userId,

            @Schema(description = "로그인 ID", example = "headoffice01")
            String loginId,

            @Schema(description = "직원명", example = "이인철")
            String name,

            @Schema(description = "역할명 목록", example = "[\"HEAD_OFFICE\"]")
            List<String> roleNames,

            @Schema(description = "활성 여부", example = "true")
            Boolean enabled,

            @Schema(description = "문자 발송 권한 보유 여부", example = "false")
            Boolean canSendSms
    ) {
    }
}