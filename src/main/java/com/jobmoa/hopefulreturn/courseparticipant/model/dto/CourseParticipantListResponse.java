package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "수강생 목록 응답")
public record CourseParticipantListResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    @Schema(description = "수강생 목록 항목")
    public record Item(
            @Schema(description = "수강 정보 ID", example = "101")
            Long courseParticipantId,

            @Schema(description = "참여자명", example = "김철수")
            String participantName,

            @Schema(description = "전화번호", example = "010-5678-1234")
            String phone,

            @Schema(description = "수강 상태", example = "APPLIED")
            String status,

            @Schema(description = "상담사 배정 목록 (상담사 + 사전/사후 구분)")
            List<CounselorSummary> counselors
    ) {
    }
}
