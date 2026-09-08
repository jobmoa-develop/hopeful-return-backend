package com.jobmoa.hopefulreturn.courseopenreminder.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffListResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.service.CourseDailyStaffService;
import com.jobmoa.hopefulreturn.courseopenreminder.entity.CourseOpenReminderConfigEntity;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.CourseOpenReminderRunResult;
import com.jobmoa.hopefulreturn.courseopenreminder.service.CourseOpenReminderMessageBuilder.SessionDate;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.CourseStaffSmsEntity;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffNotifyType;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffSmsSendStatus;
import com.jobmoa.hopefulreturn.coursestaffsms.repository.CourseStaffSmsRepository;
import com.jobmoa.hopefulreturn.sms.SmsSendCommand;
import com.jobmoa.hopefulreturn.sms.SmsSendResult;
import com.jobmoa.hopefulreturn.sms.SmsService;
import com.jobmoa.hopefulreturn.sms.support.SmsByteCalculator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 개강 하루전 자동 문자 배치 발송기.
 *
 * <p>대상 = 상태 CLOSED(모집마감) 이고 day1_date == (오늘 + days_before) 인 회차의 배정 인력 중
 * PL(PROJECT_LEADER)·진행자(STAFF)·강사(LECTURER). 상담사·PM·행정은 제외한다.
 * 발송은 <b>사용자(userId) 단위 1건</b>이며, 같은 사람이 오전·오후를 겸하면 일정에 (오전)/(오후) 를
 * 붙여 한 건으로 안내한다(오전/오후 강사가 서로 다른 사람이면 자연히 별도 발송).
 * 이력은 기존 {@code course_staff_sms} 에 notify_type=COURSE_OPEN_REMINDER 로 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseOpenReminderSender {

    /** 발송 대상 인력 역할(상담사·PM·행정 제외). */
    private static final Set<String> TARGET_ROLES = Set.of("PROJECT_LEADER", "STAFF", "LECTURER");

    private final CourseRepository courseRepository;
    private final CourseDailyStaffService courseDailyStaffService;
    private final CourseStaffSmsRepository courseStaffSmsRepository;
    private final CourseOpenReminderMessageBuilder messageBuilder;
    private final SmsService smsService;
    private final Clock clock;

    /**
     * 대상 회차·인력을 조회해 안내 문자를 발송하고 이력을 남긴다. 하루 1회 실행되며,
     * 오늘 이미 보낸 (회차, 인력) 은 이력으로 재확인해 건너뛴다(재기동·수동 재실행 방어).
     */
    public CourseOpenReminderRunResult runBatch(LocalDate today, CourseOpenReminderConfigEntity config) {
        // days_before 는 @Min(1) 로 검증되지만, 방어적으로 최소 1 로 클램프(설정값 직접 주입 대비).
        int daysBefore = Math.max(config.getDaysBefore(), 1);
        LocalDate targetOpen = today.plusDays(daysBefore);
        List<CourseEntity> courses = courseRepository.findByStatusAndDay1Date(CourseStatus.CLOSED, targetOpen);
        LocalDateTime startOfToday = today.atStartOfDay();

        int sent = 0;
        int failed = 0;
        int skipped = 0;
        // 하루 1회 배치 + 대상 회차는 특정 개강일에 한정돼 보통 수 건~수십 건 이내라
        // 회차별 findAll 조회(회차당 1건)는 실무상 허용 범위다.
        for (CourseEntity course : courses) {
            List<CourseDailyStaffListResponse.Item> items =
                    courseDailyStaffService.findAll(course.getCourseId()).assignments();
            // userId 단위 그룹핑(대상 역할만). 삭제자/상담사/PM은 findAll·필터로 제외됨.
            Map<Long, List<CourseDailyStaffListResponse.Item>> byUser = items.stream()
                    .filter(i -> i.userId() != null && TARGET_ROLES.contains(i.staffRole()))
                    .collect(Collectors.groupingBy(
                            CourseDailyStaffListResponse.Item::userId, LinkedHashMap::new, Collectors.toList()));
            String regionName = course.getRegion() != null ? course.getRegion().getName() : null;

            for (Map.Entry<Long, List<CourseDailyStaffListResponse.Item>> entry : byUser.entrySet()) {
                Long userId = entry.getKey();
                List<CourseDailyStaffListResponse.Item> userItems = entry.getValue();
                CourseDailyStaffListResponse.Item head = userItems.get(0);
                String phone = head.phone();
                if (phone == null || phone.isBlank()) {
                    skipped++;
                    log.info("[OpenReminder] 전화번호 없음 skip courseId={} userId={}", course.getCourseId(), userId);
                    continue;
                }
                if (courseStaffSmsRepository.existsByCourseIdAndUserIdAndNotifyTypeAndSentAtAfter(
                        course.getCourseId(), userId, StaffNotifyType.COURSE_OPEN_REMINDER, startOfToday)) {
                    skipped++;
                    continue;
                }
                List<SessionDate> schedule = userItems.stream()
                        .map(i -> new SessionDate(i.scheduleDate(), i.sessionType()))
                        .toList();
                String body = messageBuilder.build(regionName, course.getLocalCourseNumber(),
                        course.getCourseNumber(), head.name(), schedule, course.getLocation(), daysBefore);
                StaffSmsSendStatus status = sendOne(body, phone);
                persist(course.getCourseId(), userId, body, status);
                if (status == StaffSmsSendStatus.SUCCESS) {
                    sent++;
                } else {
                    failed++;
                }
            }
        }
        return new CourseOpenReminderRunResult(courses.size(), sent, failed, skipped);
    }

    private StaffSmsSendStatus sendOne(String body, String phone) {
        String format = SmsByteCalculator.resolveTextFormat(SmsByteCalculator.byteLength(body));
        try {
            SmsSendResult result = smsService.send(new SmsSendCommand(
                    format, null, body, List.of(new SmsSendCommand.Recipient(phone, body)), null, null, null));
            return result.success() ? StaffSmsSendStatus.SUCCESS : StaffSmsSendStatus.FAIL;
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.SMS_SEND_FAILED) {
                throw e;
            }
            log.warn("[OpenReminder] 발송 실패 — FAIL 로 기록", e);
            return StaffSmsSendStatus.FAIL;
        }
    }

    private void persist(Long courseId, Long userId, String body, StaffSmsSendStatus status) {
        LocalDateTime now = LocalDateTime.now(clock);
        courseStaffSmsRepository.save(CourseStaffSmsEntity.builder()
                .courseId(courseId)
                .userId(userId)
                .sentBy(null)
                .notifyType(StaffNotifyType.COURSE_OPEN_REMINDER)
                .content(body)
                .sendStatus(status)
                .sentAt(now)
                .createdAt(now)
                .build());
    }
}
