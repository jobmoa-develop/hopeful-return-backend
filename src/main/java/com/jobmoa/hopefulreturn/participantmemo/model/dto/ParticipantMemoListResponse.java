package com.jobmoa.hopefulreturn.participantmemo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "상담 메모 목록 응답")
public record ParticipantMemoListResponse(
        List<Item> content
) {

    @Schema(description = "상담 메모 목록 항목")
    public record Item(
            @Schema(description = "상담 메모 ID", example = "8")
            Long memoId,

            @Schema(description = "작성자명", example = "홍길동")
            String writerName,

            @Schema(description = "상담 메모 내용", example = "금일 유선 상담 완료.")
            String content,

            @Schema(description = "등록일시", example = "2026-08-20T15:20:10")
            LocalDateTime createdAt
    ) {
    }
}
