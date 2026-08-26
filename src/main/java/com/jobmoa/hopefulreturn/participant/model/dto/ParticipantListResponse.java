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
            String phone,

            @Schema(description = "참여자 매치키 (표시용 참여자ID — {이니셜}_{생년}_{전화뒤4})", example = "KCS_1978_1234")
            String matchKey,

            @Schema(description = "이 행의 수강건 ID (수강 이력이 없는 참여자 행이면 null) — 목록 행 구분/상세 이동 키", example = "102")
            Long courseParticipantId,

            @Schema(description = "이 행의 수강건 요약 (수강 이력이 없으면 null)")
            EnrollmentSummary latestEnrollment
    ) {
    }
}
