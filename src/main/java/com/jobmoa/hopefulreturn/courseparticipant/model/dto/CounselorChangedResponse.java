package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "상담사 변경 응답")
public record CounselorChangedResponse(
        @Schema(description = "수강 정보 ID", example = "101")
        Long courseParticipantId,

        @Schema(description = "변경된 상담사 배정 목록")
        List<CounselorSummary> counselors
) {
}
