package com.jobmoa.hopefulreturn.courseopenreminder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffListResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.service.CourseDailyStaffService;
import com.jobmoa.hopefulreturn.courseopenreminder.entity.CourseOpenReminderConfigEntity;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.CourseOpenReminderRunResult;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.CourseStaffSmsEntity;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffNotifyType;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffSmsSendStatus;
import com.jobmoa.hopefulreturn.coursestaffsms.repository.CourseStaffSmsRepository;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.sms.SmsSendCommand;
import com.jobmoa.hopefulreturn.sms.SmsSendResult;
import com.jobmoa.hopefulreturn.sms.SmsService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 개강 하루전 자동 문자 발송기 단위 테스트 —
 * 대상 역할 필터(PL·진행자·강사), 전화번호 없음 skip, 이력 dedup skip, userId 단위 발송·이력 저장 검증.
 */
@ExtendWith(MockitoExtension.class)
class CourseOpenReminderSenderTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 8);
    private static final LocalDate TARGET_OPEN = LocalDate.of(2026, 9, 9);
    private static final long COURSE_ID = 100L;

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseDailyStaffService courseDailyStaffService;
    @Mock
    private CourseStaffSmsRepository courseStaffSmsRepository;
    @Mock
    private SmsService smsService;

    private CourseOpenReminderSender sender() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(KST).toInstant(), KST);
        return new CourseOpenReminderSender(courseRepository, courseDailyStaffService,
                courseStaffSmsRepository, new CourseOpenReminderMessageBuilder(), smsService, clock);
    }

    private CourseEntity course() {
        return CourseEntity.builder()
                .courseId(COURSE_ID).courseNumber(58).localCourseNumber(22)
                .location("양천 교육장 A").status(CourseStatus.CLOSED)
                .region(RegionEntity.builder().name("양천").build())
                .build();
    }

    private CourseDailyStaffListResponse.Item item(String role, String session, long userId,
            String name, String phone) {
        return new CourseDailyStaffListResponse.Item(
                userId, TARGET_OPEN, role, session, userId, name, phone, null);
    }

    private CourseOpenReminderConfigEntity config() {
        return CourseOpenReminderConfigEntity.builder().enabled(true).daysBefore(1).build();
    }

    @Test
    @DisplayName("PL·진행자·강사만 발송하고 상담사·PM은 제외, 전화번호 없음·이미 발송은 건너뛴다")
    void sendsOnlyTargetRolesWithDedupAndSkip() {
        when(courseRepository.findByStatusAndDay1Date(CourseStatus.CLOSED, TARGET_OPEN))
                .thenReturn(List.of(course()));
        when(courseDailyStaffService.findAll(COURSE_ID)).thenReturn(
                new CourseDailyStaffListResponse(COURSE_ID, List.of(
                        item("LECTURER", "AM", 1L, "이강사", "01011112222"),
                        item("STAFF", "FULL", 2L, "김진행", "01033334444"),
                        item("COUNSELOR", "FULL", 3L, "상담사", "01055556666"),
                        item("PROJECT_MANAGER", "FULL", 4L, "피엠", "01077778888"),
                        item("LECTURER", "PM", 5L, "박강사", null),
                        item("PROJECT_LEADER", "FULL", 6L, "리더", "01099990000"))));
        // user 6 은 오늘 이미 발송된 것으로(dedup) 처리
        lenient().when(courseStaffSmsRepository.existsByCourseIdAndUserIdAndNotifyTypeAndSentAtAfter(
                eq(COURSE_ID), eq(6L), eq(StaffNotifyType.COURSE_OPEN_REMINDER), any())).thenReturn(true);
        when(smsService.send(any())).thenReturn(new SmsSendResult(true, "202", "success", "req", List.of()));

        CourseOpenReminderRunResult result = sender().runBatch(TODAY, config());

        // user1(강사), user2(진행자)만 발송. user3·4 역할제외, user5 전화없음, user6 dedup.
        assertThat(result.targetCourses()).isEqualTo(1);
        assertThat(result.sent()).isEqualTo(2);
        assertThat(result.failed()).isZero();
        assertThat(result.skipped()).isEqualTo(2);
        verify(smsService, times(2)).send(any());

        ArgumentCaptor<CourseStaffSmsEntity> captor = ArgumentCaptor.forClass(CourseStaffSmsEntity.class);
        verify(courseStaffSmsRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(e -> {
            assertThat(e.getNotifyType()).isEqualTo(StaffNotifyType.COURSE_OPEN_REMINDER);
            assertThat(e.getSendStatus()).isEqualTo(StaffSmsSendStatus.SUCCESS);
            assertThat(e.getSentBy()).isNull();
            assertThat(e.getContent()).contains("양천(22)_58 개강 하루 전입니다.");
        });
    }

    @Test
    @DisplayName("강사 본문에는 오전/오후 세션이 일정에 표기되고 LMS 로 발송된다")
    void lecturerSessionAnnotatedInBody() {
        when(courseRepository.findByStatusAndDay1Date(CourseStatus.CLOSED, TARGET_OPEN))
                .thenReturn(List.of(course()));
        when(courseDailyStaffService.findAll(COURSE_ID)).thenReturn(
                new CourseDailyStaffListResponse(COURSE_ID, List.of(
                        item("LECTURER", "AM", 1L, "이강사", "01011112222"))));
        when(smsService.send(any())).thenReturn(new SmsSendResult(true, "202", "success", "req", List.of()));

        sender().runBatch(TODAY, config());

        ArgumentCaptor<SmsSendCommand> captor = ArgumentCaptor.forClass(SmsSendCommand.class);
        verify(smsService).send(captor.capture());
        SmsSendCommand cmd = captor.getValue();
        assertThat(cmd.content()).contains("일정: 9/9(오전)");
        assertThat(cmd.type()).isEqualTo("LMS");
    }

    @Test
    @DisplayName("대상 회차가 없으면 아무것도 발송하지 않는다")
    void noTargetCourses() {
        when(courseRepository.findByStatusAndDay1Date(CourseStatus.CLOSED, TARGET_OPEN))
                .thenReturn(List.of());

        CourseOpenReminderRunResult result = sender().runBatch(TODAY, config());

        assertThat(result.targetCourses()).isZero();
        assertThat(result.sent()).isZero();
        verify(smsService, times(0)).send(any());
    }
}
