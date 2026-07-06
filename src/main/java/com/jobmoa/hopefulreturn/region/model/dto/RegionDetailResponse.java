package com.jobmoa.hopefulreturn.region.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "지역 상세 응답")
public record RegionDetailResponse(
        @Schema(description = "지역 ID", example = "2")
        Long regionId,

        @Schema(description = "지역명", example = "양천")
        String regionName,

        @Schema(description = "지역 레벨", example = "LEVEL2")
        String level,

        @Schema(description = "상위 지역 ID", example = "1")
        Long parentRegionId,

        @Schema(description = "상위 지역명", example = "서울")
        String parentRegionName,

        @Schema(description = "생성 일시", example = "2026-07-02T09:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 일시", example = "2026-07-02T09:00:00")
        LocalDateTime updatedAt
) {
}
