package com.jobmoa.hopefulreturn.attendance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalTime;

@Schema(description = "출석 수정 요청")
public record UpdateAttendanceRequest(
        @Schema(description = "입실 시각", example = "09:03:10")
        LocalTime checkInTime,

        @Schema(description = "퇴실 시각", example = "18:00:00")
        LocalTime checkOutTime,

        @Schema(description = "결석 처리 여부(수기). true 면 입·퇴실 시각이 지워지고 상태가 결석(ABSENT)으로 변경된다. false 면 결석 사유를 해제한다.", example = "false")
        Boolean absent,

        @Schema(description = "결석 사유(absent=true 일 때만 저장)", example = "개인 사정")
        String absenceReason
) {
}
