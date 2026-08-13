package com.jobmoa.hopefulreturn.counselingschedule.model.dto;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.staffschedule.entity.StaffScheduleEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "상담 일정 조회 응답 (상담 시작일 기준)")
public record CounselingScheduleResponse(
        @Schema(description = "상담 일정 항목 목록 (상담 시작일 오름차순)")
        List<Item> schedules,
        @Schema(description = "상담사 본인 근무 불가일 목록 (지역·회차 필터 없을 때만 채움)")
        List<UnavailabilityItem> unavailabilities
) {

    public static CounselingScheduleResponse from(
            List<CourseParticipantCounselorEntity> rows,
            List<StaffScheduleEntity> unavailRows) {
        return new CounselingScheduleResponse(
                rows.stream().map(Item::from).toList(),
                unavailRows.stream().map(UnavailabilityItem::from).toList());
    }

    public static CounselingScheduleResponse from(List<CourseParticipantCounselorEntity> rows) {
        return from(rows, List.of());
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

    @Schema(description = "상담사 본인 근무 불가일 항목")
    public record UnavailabilityItem(
            Long counselorId,
            String counselorName,
            @Schema(description = "불가 날짜(YYYY-MM-DD)")
            LocalDate date,
            @Schema(description = "시간대 — AM(오전) / PM(오후) / FULL(종일)")
            String sessionType,
            @Schema(description = "사유(비고)")
            String memo
    ) {
        public static UnavailabilityItem from(StaffScheduleEntity ss) {
            return new UnavailabilityItem(
                    ss.getUserId(),
                    ss.getUser() == null ? null : ss.getUser().getName(),
                    ss.getScheduleDate(),
                    ss.getSessionType() == null ? null : ss.getSessionType().name(),
                    ss.getMemo());
        }
    }
}
