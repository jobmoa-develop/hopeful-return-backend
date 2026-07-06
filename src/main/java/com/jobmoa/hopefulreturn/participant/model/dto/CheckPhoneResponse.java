package com.jobmoa.hopefulreturn.participant.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참여자 전화번호 중복 확인 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckPhoneResponse(
        @Schema(description = "중복 여부", example = "true")
        boolean duplicate,

        @Schema(description = "중복 시 기존 참여자 ID", example = "25")
        Long participantId
) {
}
