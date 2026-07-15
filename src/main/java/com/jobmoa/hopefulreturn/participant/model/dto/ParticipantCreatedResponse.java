package com.jobmoa.hopefulreturn.participant.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참여자 등록 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParticipantCreatedResponse(
        @Schema(description = "생성된 참여자 ID", example = "25")
        Long participantId,

        @Schema(description = "참여자 매치키 (표시용 참여자ID — {이니셜}_{생년}_{전화뒤4})", example = "KCS_1978_1234")
        String matchKey,

        @Schema(description = "함께 생성된 수강 정보 ID (enrollment 없이 등록하면 미포함)", example = "101")
        Long courseParticipantId
) {
}
