package com.jobmoa.hopefulreturn.calendarfeed.service;

import com.jobmoa.hopefulreturn.calendarfeed.IcsWriter;
import com.jobmoa.hopefulreturn.calendarfeed.SessionTimeMapping;
import com.jobmoa.hopefulreturn.calendarfeed.model.dto.CalendarFeedStatusResponse;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.StaffScheduleListResponse;
import com.jobmoa.hopefulreturn.staffschedule.service.StaffScheduleService;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarFeedServiceImpl implements CalendarFeedService {

    // 피드 조회 창: 과거 3개월 ~ 미래 12개월(초광범위 방지)
    private static final int WINDOW_PAST_MONTHS = 3;
    private static final int WINDOW_FUTURE_MONTHS = 12;
    private static final int TOKEN_BYTES = 32; // URL-safe base64 → 43자
    private static final String UID_DOMAIN = "@hopeful-return";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsersRepository usersRepository;
    private final StaffScheduleService staffScheduleService;
    private final CourseRepository courseRepository;

    @Value("${app.calendar-feed.base-url}")
    private String baseUrl;

    @Override
    @Transactional(readOnly = true)
    public CalendarFeedStatusResponse getStatus(Long userId) {
        return toStatus(loadUser(userId).getCalendarFeedToken());
    }

    @Override
    @Transactional
    public CalendarFeedStatusResponse enable(Long userId) {
        UsersEntity user = loadUser(userId);
        String token = generateToken();
        user.setCalendarFeedToken(token);
        usersRepository.save(user);
        return toStatus(token);
    }

    @Override
    @Transactional
    public void disable(Long userId) {
        UsersEntity user = loadUser(userId);
        user.setCalendarFeedToken(null);
        usersRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> buildIcsFeed(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<UsersEntity> found = usersRepository.findByCalendarFeedTokenAndDeletedFalse(token);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        UsersEntity user = found.get();

        LocalDate today = LocalDate.now();
        StaffScheduleListResponse schedule = staffScheduleService.findMy(
                user.getUserId(), today.minusMonths(WINDOW_PAST_MONTHS), today.plusMonths(WINDOW_FUTURE_MONTHS));
        List<StaffScheduleListResponse.Item> items = schedule.content();

        // 배정 회차의 교육 시각·장소를 한 번에 조회(N+1 방지)
        List<Long> courseIds = items.stream()
                .map(StaffScheduleListResponse.Item::courseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, CourseEntity> courses = courseIds.isEmpty()
                ? Map.of()
                : courseRepository.findAllById(courseIds).stream()
                        .collect(Collectors.toMap(CourseEntity::getCourseId, c -> c));

        List<IcsWriter.IcsEvent> events = new ArrayList<>();
        for (StaffScheduleListResponse.Item item : items) {
            // courseId 가 null(가용성 블록)이면 불변맵 get(null) NPE 를 피하려 조회하지 않는다.
            CourseEntity course = item.courseId() == null ? null : courses.get(item.courseId());
            events.add(toEvent(item, course));
        }

        String owner = user.getName() == null || user.getName().isBlank() ? "내" : user.getName();
        String ics = IcsWriter.write(owner + " 일정 (희망리턴)", LocalDateTime.now(ZoneOffset.UTC), events);
        return Optional.of(ics);
    }

    private IcsWriter.IcsEvent toEvent(StaffScheduleListResponse.Item item, CourseEntity course) {
        LocalDate date = item.scheduleDate();
        LocalTime startTime;
        LocalTime endTime;
        if (course != null && course.getEducationStartTime() != null && course.getEducationEndTime() != null) {
            startTime = course.getEducationStartTime();
            endTime = course.getEducationEndTime();
        } else {
            startTime = SessionTimeMapping.startOf(item.sessionType());
            endTime = SessionTimeMapping.endOf(item.sessionType());
        }
        return new IcsWriter.IcsEvent(
                buildUid(item),
                LocalDateTime.of(date, startTime),
                LocalDateTime.of(date, endTime),
                buildSummary(item),
                course == null ? null : course.getLocation(),
                buildDescription(item));
    }

    private String buildSummary(StaffScheduleListResponse.Item item) {
        if (item.courseName() != null) {
            String role = roleLabel(item.courseStaffRole());
            return role == null ? item.courseName() : item.courseName() + " · " + role;
        }
        return Boolean.FALSE.equals(item.isAvailable()) ? "근무 불가" : "근무 가능";
    }

    private String buildDescription(StaffScheduleListResponse.Item item) {
        List<String> lines = new ArrayList<>();
        if (item.courseName() != null) {
            if (item.courseStaffRole() != null) {
                lines.add("역할: " + roleLabel(item.courseStaffRole()));
            }
            if (item.courseStatus() != null) {
                lines.add("상태: " + item.courseStatus());
            }
        } else {
            lines.add(Boolean.FALSE.equals(item.isAvailable()) ? "근무 불가" : "근무 가능");
        }
        if (item.memo() != null && !item.memo().isBlank()) {
            lines.add("비고: " + item.memo());
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    // 안정적 UID: 실제 staff_schedule 행은 PK 기반, 상담사 합성행은 courseStaffId+날짜 기반.
    private String buildUid(StaffScheduleListResponse.Item item) {
        if (item.staffScheduleId() != null) {
            return "ss-" + item.staffScheduleId() + UID_DOMAIN;
        }
        return "cdc-" + item.courseStaffId() + "-" + item.scheduleDate() + UID_DOMAIN;
    }

    private String roleLabel(String role) {
        if (role == null) {
            return null;
        }
        return switch (role) {
            case "LECTURER" -> "강의";
            case "COUNSELOR" -> "상담";
            case "STAFF" -> "스태프";
            case "PROJECT_MANAGER" -> "PM";
            case "PROJECT_LEADER" -> "PL";
            case "ADMIN_STAFF" -> "행정";
            default -> role;
        };
    }

    private UsersEntity loadUser(Long userId) {
        return usersRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private CalendarFeedStatusResponse toStatus(String token) {
        boolean enabled = token != null && !token.isBlank();
        return new CalendarFeedStatusResponse(enabled, enabled ? feedUrl(token) : null);
    }

    private String feedUrl(String token) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/api/calendar/feed/" + token + ".ics";
    }

    private String generateToken() {
        byte[] buf = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
