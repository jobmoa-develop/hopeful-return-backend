package com.jobmoa.hopefulreturn.participant.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "참여자 상세 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParticipantResponse(
        @Schema(description = "참여자 ID", example = "25")
        Long participantId,

        @Schema(description = "참여자명", example = "김철수")
        String name,

        @Schema(description = "출생연도", example = "1978")
        Integer birthYear,

        @Schema(description = "전화번호", example = "010-5678-1234")
        String phone,

        @Schema(description = "생성 일시", example = "2026-08-01T09:30:22")
        LocalDateTime createdAt
) {
}
