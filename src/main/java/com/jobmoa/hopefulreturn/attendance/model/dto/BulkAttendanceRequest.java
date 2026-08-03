package com.jobmoa.hopefulreturn.attendance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "일차별 출석 일괄 등록 요청")
public record BulkAttendanceRequest(
        @Schema(description = "강좌 ID", example = "10")
        @NotNull
        Long courseId,

        @Schema(description = "수업 차수(일차)", example = "1")
        @NotNull
        Integer dayNo,

        @Schema(description = "참여자별 출결 목록")
        @NotEmpty
        @Valid
        List<Item> attendances
) {

    @Schema(description = "일괄 출석 항목")
    public record Item(
            @Schema(description = "수강 정보 ID", example = "101")
            @NotNull
            Long courseParticipantId,

            @Schema(description = "입실 시각", example = "08:55:10")
            LocalTime checkInTime,

            @Schema(description = "퇴실 시각", example = "18:01:00")
            LocalTime checkOutTime

    ) {
    }
}
