package com.jobmoa.hopefulreturn.dashboard.service;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.dashboard.model.dto.DashboardCalendarResponse;
import com.jobmoa.hopefulreturn.dashboard.model.dto.DashboardRegionStatsResponse;
import com.jobmoa.hopefulreturn.followup.entity.FollowUpEntity;
import com.jobmoa.hopefulreturn.followup.repository.FollowUpRepository;
import com.jobmoa.hopefulreturn.followupcounsel.entity.FollowUpCounselEntity;
import com.jobmoa.hopefulreturn.followupcounsel.repository.FollowUpCounselRepository;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.region.entity.RegionLevel;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 집계 서비스.
 * ※ "연락 두절 기준(5회)" 등 일부 임계값·상태 문자열은 화면 시안을 근거로 한 임시 기준이며,
 *   운영 정책이 확정되면 상수(THRESHOLD_*)만 조정하면 되도록 분리해뒀다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final int CONTACT_ATTEMPT_ALERT_THRESHOLD = 5;

    private final RegionRepository regionRepository;
    private final CourseRepository courseRepository;
    private final CourseParticipantRepository courseParticipantRepository;
    private final FollowUpRepository followUpRepository;
    private final FollowUpCounselRepository followUpCounselRepository;

    @Override
    public DashboardRegionStatsResponse getRegionStats() {
        // 강좌는 리프 레벨(OPERATION) 지역에 연결되므로, 리프 지역 전체를 기준으로 한다
        // (강좌 0건인 지역도 표에 노출하기 위해 지역 목록을 먼저 순회)
        List<RegionEntity> leafRegions = regionRepository.findByLevel(RegionLevel.OPERATION);
        List<CourseEntity> allCourses = courseRepository.findAll();
        List<CourseParticipantEntity> allParticipants = courseParticipantRepository.findAll();

        Map<Long, Long> courseIdToRegionId = allCourses.stream()
                .collect(Collectors.toMap(CourseEntity::getCourseId, CourseEntity::getRegionId));

        Map<Long, List<CourseEntity>> coursesByRegion = allCourses.stream()
                .collect(Collectors.groupingBy(CourseEntity::getRegionId));

        Map<Long, List<CourseParticipantEntity>> participantsByRegion = allParticipants.stream()
                .filter(cp -> courseIdToRegionId.containsKey(cp.getCourseId()))
                .collect(Collectors.groupingBy(cp -> courseIdToRegionId.get(cp.getCourseId())));

        List<DashboardRegionStatsResponse.Item> items = leafRegions.stream()
                .map(region -> buildRegionItem(region, coursesByRegion, participantsByRegion))
                .sorted(Comparator.comparing(DashboardRegionStatsResponse.Item::regionName))
                .toList();

        DashboardRegionStatsResponse.Totals totals = new DashboardRegionStatsResponse.Totals(
                items.stream().mapToLong(DashboardRegionStatsResponse.Item::plannedCount).sum(),
                items.stream().mapToLong(DashboardRegionStatsResponse.Item::recruitingCount).sum(),
                items.stream().mapToLong(DashboardRegionStatsResponse.Item::inProgressCount).sum(),
                items.stream().mapToLong(DashboardRegionStatsResponse.Item::canceledCount).sum(),
                items.stream().mapToLong(DashboardRegionStatsResponse.Item::completedParticipants).sum(),
                items.stream().mapToLong(DashboardRegionStatsResponse.Item::incompleteParticipants).sum());

        return new DashboardRegionStatsResponse(items, totals);
    }

    @Override
    public DashboardCalendarResponse getCalendar(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int effectiveYear = (year == null) ? today.getYear() : year;
        int effectiveMonth = (month == null) ? today.getMonthValue() : month;

        YearMonth targetMonth = YearMonth.of(effectiveYear, effectiveMonth);
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        List<DashboardCalendarResponse.Item> items = new ArrayList<>();

        items.addAll(buildTaskItems(
                courseRepository.findByRecruitStartBetween(monthStart, monthEnd),
                CourseEntity::getRecruitStart, "RECRUIT_START", "모집 시작"));
        items.addAll(buildTaskItems(
                courseRepository.findByRecruitEndBetween(monthStart, monthEnd),
                CourseEntity::getRecruitEnd, "RECRUIT_END", "모집 마감"));
        items.addAll(buildTaskItems(
                courseRepository.findByPlanSubmitDateBetween(monthStart, monthEnd),
                CourseEntity::getPlanSubmitDate, "PLAN_SUBMIT", "계획서 제출"));

        // 오늘이 조회 대상 월에 포함될 때만 경고 항목을 오늘 날짜에 병합
        boolean includesToday = !today.isBefore(monthStart) && !today.isAfter(monthEnd);
        if (includesToday) {
            items.addAll(buildAlertItems(today));
        }

        items.sort(Comparator.comparing(DashboardCalendarResponse.Item::date));
        return new DashboardCalendarResponse(items);
    }

    // ── 헬퍼 ─────────────────────────────────────────

    private DashboardRegionStatsResponse.Item buildRegionItem(
            RegionEntity region,
            Map<Long, List<CourseEntity>> coursesByRegion,
            Map<Long, List<CourseParticipantEntity>> participantsByRegion) {

        List<CourseEntity> courses = coursesByRegion.getOrDefault(region.getRegionId(), List.of());
        List<CourseParticipantEntity> participants =
                participantsByRegion.getOrDefault(region.getRegionId(), List.of());

        long planned = courses.stream().filter(c -> c.getStatus() == CourseStatus.PLANNED).count();
        long recruiting = courses.stream()
                .filter(c -> c.getStatus() == CourseStatus.OPEN || c.getStatus() == CourseStatus.RECRUITING)
                .count();
        long inProgress = courses.stream().filter(c -> c.getStatus() == CourseStatus.IN_PROGRESS).count();
        long canceled = courses.stream().filter(c -> c.getStatus() == CourseStatus.CANCELED).count();

        long completed = participants.stream()
                .filter(cp -> cp.getStatus() == CourseParticipantStatus.COMPLETED)
                .count();
        long incomplete = participants.stream()
                .filter(cp -> cp.getStatus() == CourseParticipantStatus.INCOMPLETE)
                .count();

        return new DashboardRegionStatsResponse.Item(
                region.getRegionId(),
                region.getName(),
                planned,
                recruiting,
                inProgress,
                canceled,
                completed,
                incomplete);
    }

    private List<DashboardCalendarResponse.Item> buildTaskItems(
            List<CourseEntity> courses,
            java.util.function.Function<CourseEntity, LocalDate> dateExtractor,
            String taskType,
            String label) {
        return courses.stream()
                .map(course -> {
                    LocalDate date = dateExtractor.apply(course);
                    String regionName = course.getRegion() != null ? course.getRegion().getName() : "";
                    String title = regionName + " " + course.getLocalCourseNumber() + "회차 — " + label;
                    String meta = label + "일: " + date;

                    return new DashboardCalendarResponse.Item(
                            "task-course-" + course.getCourseId() + "-" + taskType,
                            date,
                            "TASK",
                            taskType,
                            title,
                            meta,
                            "COURSE",
                            course.getCourseId(),
                            null,
                            null);
                })
                .toList();
    }

    private List<DashboardCalendarResponse.Item> buildAlertItems(LocalDate today) {
        List<DashboardCalendarResponse.Item> alerts = new ArrayList<>();

        // 1) 사전상담 연락 두절
        long contactLostCount = courseParticipantRepository
                .findByContactAttemptGreaterThanEqualAndStatusNotIn(
                        CONTACT_ATTEMPT_ALERT_THRESHOLD,
                        List.of(CourseParticipantStatus.CANCELED, CourseParticipantStatus.COMPLETED))
                .size();
        if (contactLostCount > 0) {
            alerts.add(new DashboardCalendarResponse.Item(
                    "alert-contact-lost",
                    today,
                    "ALERT",
                    null,
                    "사전상담 연락 두절",
                    CONTACT_ATTEMPT_ALERT_THRESHOLD + "회 연락 실패 · 지역담당자 보고 필요",
                    "COURSE_PARTICIPANT_GROUP",
                    null,
                    "danger",
                    contactLostCount));
        }

        // 2) 수료 처리 기한 임박 (마지막 교육일이 지났는데 아직 CONFIRMED 상태로 남아있는 건)
        long pendingCompletionCount = courseParticipantRepository
                .findByStatusIn(List.of(CourseParticipantStatus.CONFIRMED)).stream()
                .filter(cp -> cp.getCourse() != null)
                .filter(cp -> {
                    LocalDate lastDay = lastEducationDate(cp.getCourse());
                    return lastDay != null && !lastDay.isAfter(today);
                })
                .count();
        if (pendingCompletionCount > 0) {
            alerts.add(new DashboardCalendarResponse.Item(
                    "alert-completion-pending",
                    today,
                    "ALERT",
                    null,
                    "수료 처리 기한 임박",
                    "교육종료 후 처리 마감 임박",
                    "COURSE_PARTICIPANT_GROUP",
                    null,
                    "danger",
                    pendingCompletionCount));
        }

        // 3) 사후관리 상담 2회 이상인데 아직 취업 미확정 (무응답 근사치)
        //    V11 재편: 접촉 로그는 follow_up_counsel 로 분리됐고, follow_up 은 취업일 스냅샷이다.
        Set<Long> employedParticipantIds = followUpRepository.findAll().stream()
                .filter(f -> f.getEmploymentDate() != null)
                .map(FollowUpEntity::getCourseParticipantId)
                .collect(Collectors.toSet());
        Map<Long, Long> counselCountByParticipant = followUpCounselRepository.findAll().stream()
                .collect(Collectors.groupingBy(FollowUpCounselEntity::getCourseParticipantId, Collectors.counting()));
        long staleFollowUpCount = counselCountByParticipant.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .filter(e -> !employedParticipantIds.contains(e.getKey()))
                .count();
        if (staleFollowUpCount > 0) {
            alerts.add(new DashboardCalendarResponse.Item(
                    "alert-followup-stale",
                    today,
                    "ALERT",
                    null,
                    "사후관리 상담 2회 후 미취업",
                    "종료 가능 검토 대상",
                    "COURSE_PARTICIPANT_GROUP",
                    null,
                    "warn",
                    staleFollowUpCount));
        }

        return alerts;
    }

    private LocalDate lastEducationDate(CourseEntity course) {
        LocalDate[] days = {
                course.getDay5Date(), course.getDay4Date(), course.getDay3Date(),
                course.getDay2Date(), course.getDay1Date()
        };
        for (LocalDate day : days) {
            if (day != null) {
                return day;
            }
        }
        return null;
    }
}