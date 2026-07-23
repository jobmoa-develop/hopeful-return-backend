package com.jobmoa.hopefulreturn.followupcounsel.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사후관리 상담 삭제 응답")
public record FollowUpCounselDeletedResponse(
        @Schema(description = "삭제 결과 메시지", example = "사후관리 상담 정보가 삭제되었습니다.")
        String message
) {
}
