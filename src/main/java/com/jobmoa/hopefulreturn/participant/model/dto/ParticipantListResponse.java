package com.jobmoa.hopefulreturn.participant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "참여자 목록 응답")
public record ParticipantListResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    @Schema(description = "참여자 목록 항목")
    public record Item(
            @Schema(description = "참여자 ID", example = "25")
            Long participantId,

            @Schema(description = "참여자명", example = "김철수")
            String name,

            @Schema(description = "출생연도", example = "1978")
            Integer birthYear,

            @Schema(description = "전화번호", example = "010-5678-1234")
            String phone
    ) {
    }
}
