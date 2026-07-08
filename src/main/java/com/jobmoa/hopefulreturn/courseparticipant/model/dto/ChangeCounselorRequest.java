package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "상담사 변경 요청 (전체 집합 교체)")
public record ChangeCounselorRequest(
        @Schema(description = "변경할 상담사 배정 목록 (상담사 + 사전/사후 구분)")
        @NotEmpty
        @Valid
        List<CounselorAssignment> counselors
) {
}
