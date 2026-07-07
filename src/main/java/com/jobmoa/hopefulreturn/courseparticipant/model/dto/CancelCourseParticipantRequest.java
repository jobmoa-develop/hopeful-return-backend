package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "수강 취소 요청")
public record CancelCourseParticipantRequest(
        @Schema(description = "취소 사유 (incomplete_reason 에 기록)", example = "참여자 요청")
        @Size(max = 255)
        String reason
) {
}
