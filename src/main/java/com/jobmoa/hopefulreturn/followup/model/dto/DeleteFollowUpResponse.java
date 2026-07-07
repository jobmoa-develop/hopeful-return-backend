package com.jobmoa.hopefulreturn.followup.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사후관리 삭제 응답")
public record DeleteFollowUpResponse(
        @Schema(description = "삭제 결과 메시지", example = "사후관리 정보가 삭제되었습니다.")
        String message
) {
}
