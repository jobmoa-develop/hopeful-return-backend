package com.jobmoa.hopefulreturn.participantsms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "문자 발송 이력 상세 응답")
public record ParticipantSmsDetailResponse(
        @Schema(description = "문자 이력 ID", example = "501")
        Long smsId,

        @Schema(description = "수강생 ID", example = "101")
        Long courseParticipantId,

        @Schema(description = "발송 형식", example = "MMS")
        String messageFormat,

        @Schema(description = "제목", example = "수료 안내")
        String title,

        @Schema(description = "본문(치환 완료)", example = "홍길동님, 수료를 축하합니다.")
        String content,

        @Schema(description = "발송 상태(PENDING=접수·전달확인중, SUCCESS/FAIL=전달결과)", example = "SUCCESS")
        String sendStatus,

        @Schema(description = "SENS 메시지 ID(발송결과 조회로 확보, 메시지 검색용)", example = "0-ATA1-202607-...")
        String messageId,

        @Schema(description = "전달 결과 코드(SENS statusCode, 0=성공)", example = "0")
        String resultCode,

        @Schema(description = "전달 결과 사유(실패 시 원인)", example = "발신 번호 변작 방지")
        String resultMessage,

        @Schema(description = "전달 완료 일시", example = "2026-07-24T15:20:12")
        LocalDateTime completeTime,

        @Schema(description = "발송자 ID", example = "1")
        Long sentBy,

        @Schema(description = "발송자명", example = "관리자")
        String senderName,

        @Schema(description = "발송 일시(예약건은 null)", example = "2026-07-24T15:20:10")
        LocalDateTime sentAt,

        @Schema(description = "예약 발송 예정 시각(예약건만)", example = "2026-08-01T09:00:00")
        LocalDateTime reserveTime,

        @Schema(description = "SENS 예약 batch ID(예약 취소 단위)", example = "0-Reserve-...")
        String reserveId,

        @Schema(description = "생성 일시", example = "2026-07-24T15:20:10")
        LocalDateTime createdAt,

        @Schema(description = "첨부 이미지 참조(MMS)")
        List<String> imageUrls
) {
}
