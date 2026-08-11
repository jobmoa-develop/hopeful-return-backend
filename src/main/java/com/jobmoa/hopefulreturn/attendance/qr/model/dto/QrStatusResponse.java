package com.jobmoa.hopefulreturn.attendance.qr.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;
import java.util.List;

/**
 * QR 본인확인 후 오늘 출결 상태 응답 — 입·퇴실 시각, 상태, 조퇴·외출 목록과
 * 현재 시각·일정 기준으로 참여자가 수행 가능한 액션 플래그를 함께 담는다.
 * verify / check-in / leave / leave-return / check-out 이후 갱신된 상태를 반환한다.
 */
@Schema(description = "QR 오늘 출결 상태 응답")
public record QrStatusResponse(
        @Schema(description = "참여자명", example = "김철수")
        String participantName,

        @Schema(description = "오늘 교육 일차(1~5)", example = "1")
        Integer dayNo,

        @Schema(description = "입실 시각(미입실 시 null)", example = "09:03:00")
        LocalTime checkInTime,

        @Schema(description = "퇴실 시각(미퇴실 시 null)", example = "18:01:00")
        LocalTime checkOutTime,

        @Schema(description = "출결 상태", example = "ATTEND")
        String status,

        @Schema(description = "조퇴·외출 기록 목록")
        List<QrLeaveItem> leaves,

        @Schema(description = "입실 가능 여부", example = "false")
        boolean canCheckIn,

        @Schema(description = "조퇴·외출 입력 가능 여부", example = "true")
        boolean canLeave,

        @Schema(description = "퇴실 가능 여부(교육 종료 시각 이후)", example = "false")
        boolean canCheckOut
) {
}
