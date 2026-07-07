package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "수강 삭제 요청 (하드 삭제 사유)")
public record DeleteCourseParticipantRequest(
        @Schema(description = "삭제 사유", example = "참여자 요청")
        @Size(max = 255)
        String reason
) {
}
