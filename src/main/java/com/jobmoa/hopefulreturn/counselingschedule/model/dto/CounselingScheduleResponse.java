package com.jobmoa.hopefulreturn.counselingschedule.model.dto;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "상담 일정 조회 응답 (상담 시작일 기준)")
public record CounselingScheduleResponse(
        @Schema(description = "상담 일정 항목 목록 (상담 시작일 오름차순)")
        List<Item> schedules
) {

    public static CounselingScheduleResponse from(List<CourseParticipantCounselorEntity> rows) {
        return new CounselingScheduleResponse(rows.stream().map(Item::from).toList());
    }

    @Schema(description = "상담 일정 항목")
    public record Item(
            Long courseParticipantId,
            Long courseParticipantCounselorId,
            @Schema(description = "상담 시작 날짜(YYYY-MM-DD)")
            LocalDate date,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String regionName,
            Integer courseNumber,
            String participantName,
            Long counselorId,
            String counselorName,
            @Schema(description = "상담 구분 — PRE_SESSION / POST_SESSION_1 / POST_SESSION_2")
            String counselingType,
            @Schema(description = "상담 완료 여부(종료 일시 입력 시 완료)")
            boolean completed
    ) {
        public static Item from(CourseParticipantCounselorEntity r) {
            CourseParticipantEntity cp = r.getCourseParticipant();
            CourseEntity course = cp == null ? null : cp.getCourse();
            RegionEntity region = course == null ? null : course.getRegion();
            ParticipantEntity participant = cp == null ? null : cp.getParticipant();
            LocalDateTime startedAt = r.getCounselingStartedAt();
            return new Item(
                    cp == null ? null : cp.getCourseParticipantId(),
                    r.getCourseParticipantCounselorId(),
                    startedAt == null ? null : startedAt.toLocalDate(),
                    startedAt,
                    r.getCounselingEndedAt(),
                    region == null ? null : region.getName(),
                    course == null ? null : course.getCourseNumber(),
                    participant == null ? null : participant.getName(),
                    r.getCounselorId(),
                    r.getCounselor() == null ? null : r.getCounselor().getName(),
                    r.getStatus() == null ? null : r.getStatus().name(),
                    r.isCompleted());
        }
    }
}
