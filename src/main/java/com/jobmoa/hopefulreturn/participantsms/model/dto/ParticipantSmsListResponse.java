package com.jobmoa.hopefulreturn.participantsms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "문자 발송 이력 목록 응답")
public record ParticipantSmsListResponse(
        List<Item> content
) {

    @Schema(description = "문자 발송 이력 항목")
    public record Item(
            @Schema(description = "문자 이력 ID", example = "501")
            Long smsId,

            @Schema(description = "발송 형식", example = "LMS")
            String messageFormat,

            @Schema(description = "제목", example = "수료 안내")
            String title,

            @Schema(description = "본문(치환 완료)", example = "홍길동님, 수료를 축하합니다.")
            String content,

            @Schema(description = "발송 상태", example = "SUCCESS")
            String sendStatus,

            @Schema(description = "발송 일시", example = "2026-07-24T15:20:10")
            LocalDateTime sentAt,

            @Schema(description = "발송자명", example = "관리자")
            String senderName,

            @Schema(description = "첨부 이미지 참조(MMS)")
            List<String> imageUrls
    ) {
    }
}
