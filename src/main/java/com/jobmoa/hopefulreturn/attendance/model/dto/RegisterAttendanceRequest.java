package com.jobmoa.hopefulreturn.attendance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Schema(description = "출석 등록 요청")
public record RegisterAttendanceRequest(
        @Schema(description = "수강 정보 ID", example = "15")
        @NotNull
        Long courseParticipantId,

        @Schema(description = "수업 차수(일차)", example = "1")
        @NotNull
        Integer dayNo,

        @Schema(description = "입실 시각", example = "08:55:23")
        LocalTime checkInTime,

        @Schema(description = "퇴실 시각", example = "18:02:10")
        LocalTime checkOutTime,

        @Schema(description = "결석 처리 여부(수기). true 면 입·퇴실 시각은 무시되고 상태가 결석(ABSENT)으로 저장된다.", example = "false")
        Boolean absent,

        @Schema(description = "결석 사유(absent=true 일 때만 저장)", example = "개인 사정")
        String absenceReason

) {
}
