package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "상담 세션 기록 응답")
public record CounselingSessionResponse(
        @Schema(description = "수강 정보 ID", example = "101")
        Long courseParticipantId,

        @Schema(description = "상담 구분 — PRE_SESSION(사전상담) / POST_SESSION_1(사후상담1) / POST_SESSION_2(사후상담2)",
                example = "PRE_SESSION")
        String counselingType,

        @Schema(description = "상담사(사용자) ID", example = "8")
        Long counselorId,

        @Schema(description = "상담사명", example = "홍길동")
        String counselorName,

        @Schema(description = "상담 시작 일시", example = "2026-07-20T14:00:00")
        LocalDateTime startedAt,

        @Schema(description = "상담 종료 일시", example = "2026-07-20T15:00:00")
        LocalDateTime endedAt,

        @Schema(description = "상담 메모", example = "사전상담 진행. 창업 자금 관련 후속 안내 필요.")
        String memo,

        @Schema(description = "상담 완료 여부 (종료 일시 입력 시 완료)", example = "true")
        boolean completed
) {
}
