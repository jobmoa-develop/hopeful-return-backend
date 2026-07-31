package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import com.jobmoa.hopefulreturn.courseparticipant.entity.ChangeSubject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "상담 세션 기록 요청 — 일시 null 필드는 기존값을 유지한다(부분 수정)")
public record RecordCounselingSessionRequest(
        @Schema(description = "상담 시작 일시", example = "2026-07-20T14:00:00")
        LocalDateTime startedAt,

        @Schema(description = "상담 종료 일시 — 입력 시 해당 상담은 완료로 간주", example = "2026-07-20T15:00:00")
        LocalDateTime endedAt,

        @Schema(description = "상담 메모", example = "사전상담 진행. 창업 자금 관련 후속 안내 필요.")
        @Size(max = 1000)
        String memo,

        @Schema(description = "변경 주체 — NONE(빈칸) / COUNSELOR(상담사) / PARTICIPANT(참여자)", example = "COUNSELOR")
        @NotNull
        ChangeSubject changedBy,

        @Schema(description = "변경 비고(필수)", example = "일정 재조정 사유 기록")
        @NotBlank
        String reason
) {
}
