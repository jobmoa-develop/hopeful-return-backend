package com.jobmoa.hopefulreturn.participant.model.dto;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselingType;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Schema(description = "참여자 목록용 최신 수강건 요약 — 지역/회차·진행상태·상담 완료여부·출결 집계")
public record EnrollmentSummary(
        @Schema(description = "수강 정보 ID", example = "102")
        Long courseParticipantId,

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

        @Schema(description = "수강 상태", example = "CONFIRMED")
        String status,

        @Schema(description = "수료일", example = "2026-08-24")
        LocalDate completionDate,

        @Schema(description = "상담사 배정 목록 (사전/사후1/사후2 슬롯 + 세션 진행 상태)")
        List<CounselorSummary> counselors,

        @Schema(description = "사전상담 완료 여부 (PRE_SESSION 슬롯의 종료 일시 입력 여부)", example = "true")
        boolean preCounselingCompleted,

        @Schema(description = "출석 일수 (출석+지각 집계)", example = "3")
        Integer attendedDays,

        @Schema(description = "교육 총 일수 (day1~day5 중 지정된 날짜 수)", example = "5")
        Integer totalCourseDays
) {

    public static EnrollmentSummary from(
            CourseParticipantEntity cp,
            List<CourseParticipantCounselorEntity> counselorRows,
            long attendedDays) {
        CourseEntity course = cp.getCourse();
        return new EnrollmentSummary(
                cp.getCourseParticipantId(),
                cp.getCourseId(),
                course == null ? null : course.getCourseName(),
                course == null || course.getRegion() == null ? null : course.getRegion().getName(),
                course == null ? null : course.getCourseNumber(),
                course == null ? null : course.getLocalCourseNumber(),
                cp.getStatus() == null ? null : cp.getStatus().name(),
                cp.getCompletionDate(),
                counselorRows.stream().map(CounselorSummary::from).toList(),
                counselorRows.stream()
                        .anyMatch(row -> row.getStatus() == CounselingType.PRE_SESSION && row.isCompleted()),
                (int) attendedDays,
                totalCourseDays(course));
    }

    private static Integer totalCourseDays(CourseEntity course) {
        if (course == null) {
            return null;
        }
        return (int) Stream.of(
                        course.getDay1Date(), course.getDay2Date(), course.getDay3Date(),
                        course.getDay4Date(), course.getDay5Date())
                .filter(Objects::nonNull)
                .count();
    }
}
