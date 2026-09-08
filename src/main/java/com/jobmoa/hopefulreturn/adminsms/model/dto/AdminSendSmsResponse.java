package com.jobmoa.hopefulreturn.adminsms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "관리자 임의 번호 문자 발송 응답")
public record AdminSendSmsResponse(
        @Schema(description = "실제 발송 형식", example = "LMS")
        String messageFormat,

        @Schema(description = "발송 대상 수(중복제거 후)", example = "2")
        int totalCount,

        @Schema(description = "성공 건수", example = "2")
        int successCount,

        @Schema(description = "실패 건수", example = "0")
        int failedCount,

        @Schema(description = "결과 상태", example = "success")
        String statusName,

        @Schema(description = "생성된 문자 이력 ID 목록", example = "[501, 502]")
        List<Long> adminSmsIds,

        @Schema(description = "예약 발송 시 SENS 예약 batch ID(예약 취소 단위). 즉시 발송은 null", example = "0-Reserve-...")
        String reserveId
) {
}
