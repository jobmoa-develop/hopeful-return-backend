package com.jobmoa.hopefulreturn.participantsms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "문자 발송 내역 목록(페이지) 응답")
public record ParticipantSmsPageResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    @Schema(description = "문자 발송 내역 항목(목록용)")
    public record Item(
            @Schema(description = "문자 이력 ID", example = "501")
            Long smsId,

            @Schema(description = "수강생 ID", example = "101")
            Long courseParticipantId,

            @Schema(description = "수신자명", example = "홍길동")
            String participantName,

            @Schema(description = "수신자 전화번호", example = "01012345678")
            String phone,

            @Schema(description = "지역명", example = "양천")
            String regionName,

            @Schema(description = "강좌명", example = "양천5기")
            String courseName,

            @Schema(description = "회차번호", example = "5")
            Integer courseNumber,

            @Schema(description = "발송 형식", example = "LMS")
            String messageFormat,

            @Schema(description = "제목", example = "수료 안내")
            String title,

            @Schema(description = "본문(치환 완료)", example = "홍길동님, 수료를 축하합니다.")
            String content,

            @Schema(description = "발송 상태(RESERVED=예약중, PENDING=접수·전달확인중, SUCCESS/FAIL=전달결과, CANCELED=예약취소)",
                    example = "SUCCESS")
            String sendStatus,

            @Schema(description = "SENS 메시지 ID(메시지 검색용)", example = "0-ATA1-202607-...")
            String messageId,

            @Schema(description = "전달 결과 코드(SENS statusCode, 0=성공)", example = "0")
            String resultCode,

            @Schema(description = "전달 결과 사유(실패 시 원인)", example = "발신 번호 변작 방지")
            String resultMessage,

            @Schema(description = "전달 완료 일시", example = "2026-07-24T15:20:12")
            LocalDateTime completeTime,

            @Schema(description = "발송 일시(예약건은 null)", example = "2026-07-24T15:20:10")
            LocalDateTime sentAt,

            @Schema(description = "예약 발송 예정 시각(예약건만)", example = "2026-08-01T09:00:00")
            LocalDateTime reserveTime,

            @Schema(description = "SENS 예약 batch ID(예약 취소 단위)", example = "0-Reserve-...")
            String reserveId,

            @Schema(description = "발송자명", example = "관리자")
            String senderName
    ) {
    }
}
