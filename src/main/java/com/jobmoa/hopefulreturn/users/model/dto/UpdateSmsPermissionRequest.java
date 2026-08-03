package com.jobmoa.hopefulreturn.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "문자 발송 권한 설정 요청")
public record UpdateSmsPermissionRequest(
        @Schema(description = "문자 발송 권한 부여 여부", example = "true")
        @NotNull
        Boolean canSendSms
) {
}
