package com.jobmoa.hopefulreturn.coursestaffsms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "강좌 담당자 인력배정 안내 문자 발송 결과")
public record SendCourseStaffSmsResponse(
        @Schema(description = "발송 형식(SMS/LMS) — 그룹 중 하나라도 LMS 면 LMS") String messageFormat,
        @Schema(description = "발송 시도 총 인원(전화번호 없어 제외된 인원 제외)") int totalCount,
        @Schema(description = "발송 성공 인원") int successCount,
        @Schema(description = "발송 실패 인원") int failedCount,
        @Schema(description = "전화번호가 없어 발송에서 제외된 인원") List<Skipped> skipped
) {

    @Schema(description = "전화번호 없는 제외 인원")
    public record Skipped(Long userId, String name) {
    }
}
