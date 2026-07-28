package com.jobmoa.hopefulreturn.followup.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사후관리 집계 응답(수료 참여자 기준 취업/숲체험/국취연계 비율)")
public record FollowUpStatsResponse(
        @Schema(description = "집계 대상 수료 참여자 수", example = "120")
        long totalCompleted,

        @Schema(description = "취업일 등록 인원", example = "45")
        long employedCount,

        @Schema(description = "숲체험 방문일 등록 인원", example = "60")
        long forestVisitCount,

        @Schema(description = "국취연계일 등록 인원", example = "30")
        long nationalProgramCount,

        @Schema(description = "취업률(%)", example = "37.5")
        double employmentRate,

        @Schema(description = "숲체험 방문률(%)", example = "50.0")
        double forestVisitRate,

        @Schema(description = "국취연계률(%)", example = "25.0")
        double nationalProgramRate
) {
}