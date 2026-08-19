package com.jobmoa.hopefulreturn.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "메일 발송(근무불가 알림 수신) 권한 설정 요청")
public record UpdateEmailPermissionRequest(
        @Schema(description = "메일 발송 권한 부여 여부", example = "true")
        @NotNull
        Boolean canSendEmail
) {
}
