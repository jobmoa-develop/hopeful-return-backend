package com.jobmoa.hopefulreturn.region.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지역 목록 응답")
public record RegionListResponse(
        @Schema(description = "지역 ID", example = "1")
        Long regionId,

        @Schema(description = "지역명", example = "서울")
        String regionName,

        @Schema(description = "지역 레벨", example = "LEVEL1")
        String level,

        @Schema(description = "상위 지역 ID", example = "1")
        Long parentRegionId
) {
}
