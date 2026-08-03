package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "수강생 상세 응답 — 참여자 상세 페이지의 기본 데이터 (조회 키는 courseParticipantId)")
public record CourseParticipantDetailResponse(
        @Schema(description = "수강 정보 ID", example = "101")
        Long courseParticipantId,

        @Schema(description = "참여자 ID", example = "25")
        Long participantId,

        @Schema(description = "참여자명", example = "김철수")
        String participantName,

        @Schema(description = "참여자 매치키 (표시용 참여자ID — {이니셜}_{생년}_{전화뒤4})", example = "KCS_1978_1234")
        String matchKey,

        @Schema(description = "출생연도", example = "1978")
        Integer birthYear,

        @Schema(description = "전화번호", example = "010-5678-1234")
        String phone,

        @Schema(description = "강좌 ID", example = "15")
        Long courseId,

        @Schema(description = "강좌명", example = "양천5기")
        String courseName,

        @Schema(description = "지역명", example = "서울")
        String regionName,

        @Schema(description = "전체 회차", example = "5")
        Integer courseNumber,

        @Schema(description = "지역 내 회차", example = "2")
        Integer localCourseNumber,

        @Schema(description = "상담사 배정 목록 (사전/사후1/사후2 슬롯 + 세션 진행 상태)")
        List<CounselorSummary> counselors,

        @Schema(description = "수강 상태", example = "CONFIRMED")
        String status,

        @Schema(description = "연락 시도 횟수", example = "0")
        Integer contactAttempt,

        @Schema(description = "유입 경로", example = "워크넷")
        String inflowType,

        @Schema(description = "신청일", example = "2026-08-01")
        LocalDate applyDate,

        @Schema(description = "접수일", example = "2026-08-02")
        LocalDate receptionDate,

        @Schema(description = "수료일", example = "2026-08-24")
        LocalDate completionDate,

        @Schema(description = "기초교육 이수 여부", example = "Y")
        String basicEducation
) {
}
