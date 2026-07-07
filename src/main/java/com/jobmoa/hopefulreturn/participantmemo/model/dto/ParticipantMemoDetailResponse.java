package com.jobmoa.hopefulreturn.participantmemo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "상담 메모 상세 응답")
public record ParticipantMemoDetailResponse(
        @Schema(description = "상담 메모 ID", example = "8")
        Long memoId,

        @Schema(description = "수강생 ID", example = "15")
        Long courseParticipantId,

        @Schema(description = "작성자 ID", example = "4")
        Long writerId,

        @Schema(description = "작성자명", example = "홍길동")
        String writerName,

        @Schema(description = "상담 메모 내용", example = "금일 유선 상담 완료.")
        String content,

        @Schema(description = "등록일시", example = "2026-08-20T15:20:10")
        LocalDateTime createdAt
) {
}
