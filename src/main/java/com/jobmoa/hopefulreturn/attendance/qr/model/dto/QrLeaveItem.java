package com.jobmoa.hopefulreturn.attendance.qr.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

/**
 * QR 응답에서 공용으로 쓰는 조퇴·외출 항목.
 * returnTime 이 null 이면 조퇴(복귀 전), 채워져 있으면 외출(복귀 완료)로 해석한다.
 */
@Schema(description = "조퇴·외출 항목")
public record QrLeaveItem(
        @Schema(description = "조퇴·외출 ID", example = "5")
        Long attendanceLeaveId,

        @Schema(description = "나간(조퇴) 시각", example = "14:30:00")
        LocalTime leaveTime,

        @Schema(description = "복귀 시각(미복귀 시 null)", example = "15:20:00")
        LocalTime returnTime,

        @Schema(description = "사유", example = "병원 진료")
        String reason
) {
}
