package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "강좌 참여자 목록 응답")
public record CourseParticipantListResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    @Schema(description = "강좌 참여자 목록 항목")
    public record Item(
            @Schema(description = "강좌 참여자 ID", example = "10")
            Long courseParticipantId,

            @Schema(description = "참여자명", example = "김철수")
            String participantName,

            @Schema(description = "전화번호", example = "010-1234-5678")
            String phone,

            @Schema(description = "참여 상태", example = "APPLIED")
            String status
    ) {
    }
}
