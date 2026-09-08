package com.jobmoa.hopefulreturn.adminsms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "관리자 임의 문자 발송 내역 목록(페이지) 응답")
public record AdminSmsPageResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    @Schema(description = "관리자 임의 문자 발송 내역 항목(목록용)")
    public record Item(
            @Schema(description = "문자 이력 ID", example = "501")
            Long adminSmsId,

            @Schema(description = "수신 전화번호", example = "01012345678")
            String toPhone,

            @Schema(description = "수신자 라벨(선택)", example = "김담당")
            String recipientLabel,

            @Schema(description = "발송 형식", example = "LMS")
            String messageFormat,

            @Schema(description = "제목", example = "안내")
            String title,

            @Schema(description = "본문", example = "안녕하세요. 안내드립니다.")
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

            @Schema(description = "전달 완료 일시", example = "2026-09-08T15:20:12")
            LocalDateTime completeTime,

            @Schema(description = "발송 일시(예약건은 null)", example = "2026-09-08T15:20:10")
            LocalDateTime sentAt,

            @Schema(description = "예약 발송 예정 시각(예약건만)", example = "2026-09-10T09:00:00")
            LocalDateTime reserveTime,

            @Schema(description = "SENS 예약 batch ID(예약 취소 단위)", example = "0-Reserve-...")
            String reserveId,

            @Schema(description = "발송자명", example = "관리자")
            String senderName
    ) {
    }
}
