package com.jobmoa.hopefulreturn.followup.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "사후관리 목록 응답(수료 참여자 + follow_up 스냅샷 + 상담 요약, 페이지)")
public record FollowUpListResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    @Schema(description = "사후관리 목록 항목")
    public record Item(
            @Schema(description = "사후관리 ID(스냅샷 미존재 시 null)", example = "11")
            Long followUpId,

            @Schema(description = "수강생 ID", example = "15")
            Long courseParticipantId,

            @Schema(description = "참여자명", example = "김서연")
            String name,

            @Schema(description = "표시용 식별키(matchKey)", example = "김서연_010****1023")
            String matchKey,

            @Schema(description = "지역명", example = "남부")
            String regionName,

            @Schema(description = "지역 내 회차(local_course_number)", example = "3")
            Integer localCourseNumber,

            @Schema(description = "수료일", example = "2026-05-30")
            LocalDate completionDate,

            @Schema(description = "취업일", example = "2026-06-18")
            LocalDate employmentDate,

            @Schema(description = "숲체험 프로그램 참여일", example = "2026-06-05")
            LocalDate forestProgramDate,

            @Schema(description = "국민취업지원제도 연계일", example = "2026-06-20")
            LocalDate nationalProgramDate,

            @Schema(description = "국민취업지원제도 지점", example = "남부")
            String nationalProgramBranch,

            @Schema(description = "상담 로그 건수", example = "2")
            long counselCount,

            @Schema(description = "최근 상담일", example = "2026-07-10")
            LocalDate lastCounselDate
    ) {
    }
}
