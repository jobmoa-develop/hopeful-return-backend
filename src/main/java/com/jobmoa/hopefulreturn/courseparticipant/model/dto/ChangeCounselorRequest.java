package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import com.jobmoa.hopefulreturn.courseparticipant.entity.ChangeSubject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "상담사 변경 요청 (전체 집합 교체)")
public record ChangeCounselorRequest(
        @Schema(description = "변경할 상담사 배정 목록 (상담사 + 사전/사후 구분)")
        @NotEmpty
        @Valid
        List<CounselorAssignment> counselors,

        @Schema(description = "변경 주체 — NONE(빈칸) / COUNSELOR(상담사) / PARTICIPANT(참여자)", example = "PARTICIPANT")
        @NotNull
        ChangeSubject changedBy,

        @Schema(description = "변경 비고(필수)", example = "참여자 요청으로 상담사 재배정")
        @NotBlank
        String reason
) {
}
