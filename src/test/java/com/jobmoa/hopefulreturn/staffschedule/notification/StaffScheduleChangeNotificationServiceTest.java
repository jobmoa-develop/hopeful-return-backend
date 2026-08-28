package com.jobmoa.hopefulreturn.staffschedule.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.email.EmailService;
import com.jobmoa.hopefulreturn.email.dto.StaffUnavailableMail;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.staffschedule.event.StaffBecameUnavailableEvent;
import com.jobmoa.hopefulreturn.staffunavailablenotice.entity.NoticeSendStatus;
import com.jobmoa.hopefulreturn.staffunavailablenotice.entity.StaffUnavailableNoticeEntity;
import com.jobmoa.hopefulreturn.staffunavailablenotice.repository.StaffUnavailableNoticeRepository;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 근무불가 메일 알림 리스너 단위 테스트. 수신자 선정(메일 발송 권한 can_send_email 기반·이메일/삭제 필터),
 * 발송, 이력 저장, 발송 실패 격리를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffScheduleChangeNotificationServiceTest {

    private static final Long STAFF_SCHEDULE_ID = 12L;
    private static final Long COURSE_STAFF_ID = 77L;
    private static final Long COURSE_ID = 500L;
    private static final Long STAFF_USER_ID = 6L;

    @Mock
    private CourseStaffRepository courseStaffRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private StaffUnavailableNoticeRepository noticeRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private StaffScheduleChangeNotificationService service;

    private StaffBecameUnavailableEvent event() {
        return new StaffBecameUnavailableEvent(
                STAFF_SCHEDULE_ID, COURSE_STAFF_ID, STAFF_USER_ID,
                LocalDate.of(2026, 8, 25), SessionType.AM, "개인 사정");
    }

    private void stubCourseAndStaff() {
        CourseStaffEntity cs = CourseStaffEntity.builder()
                .courseStaffId(COURSE_STAFF_ID).courseId(COURSE_ID).userId(STAFF_USER_ID).build();
        when(courseStaffRepository.findById(COURSE_STAFF_ID)).thenReturn(Optional.of(cs));
        CourseEntity course = CourseEntity.builder()
                .courseId(COURSE_ID)
                .region(RegionEntity.builder().name("서울").build())
                .localCourseNumber(3)
                .courseNumber(10)
                .build();
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(usersRepository.findById(STAFF_USER_ID))
                .thenReturn(Optional.of(user(STAFF_USER_ID, "김인력", "staff@x.com", false)));
    }

    private UsersEntity user(Long id, String name, String email, boolean deleted) {
        return UsersEntity.builder().userId(id).name(name).email(email).deleted(deleted).build();
    }

    // 메일 발송 권한(can_send_email=true) 보유 사용자 목록을 수신 후보로 스텁한다.
    private void stubPermittedRecipients(UsersEntity... users) {
        when(usersRepository.findByCanSendEmailTrue()).thenReturn(List.of(users));
    }

    @Test
    @DisplayName("메일 권한 보유·이메일 있는 사용자 전원에게 발송하고 각각 이력을 저장한다")
    void notifiesAllPermitted_andRecordsHistory() {
        stubCourseAndStaff();
        stubPermittedRecipients(
                user(101L, "관리자A", "a@x.com", false),
                user(102L, "운영자B", "b@x.com", false),
                user(103L, "지역담당C", "c@x.com", false));

        service.onStaffBecameUnavailable(event());

        verify(emailService, times(3)).sendStaffUnavailableNotice(anyString(), any(StaffUnavailableMail.class));
        verify(emailService).sendStaffUnavailableNotice(eq("a@x.com"), any());
        verify(emailService).sendStaffUnavailableNotice(eq("c@x.com"), any());
        verify(noticeRepository, times(3)).save(any(StaffUnavailableNoticeEntity.class));
    }

    @Test
    @DisplayName("메일 본문은 지역·회차(localCourseNumber)·날짜·시간대·인력명을 담는다")
    void buildsMailWithCourseContext() {
        stubCourseAndStaff();
        stubPermittedRecipients(user(101L, "관리자A", "a@x.com", false));

        service.onStaffBecameUnavailable(event());

        ArgumentCaptor<StaffUnavailableMail> captor = ArgumentCaptor.forClass(StaffUnavailableMail.class);
        verify(emailService).sendStaffUnavailableNotice(eq("a@x.com"), captor.capture());
        StaffUnavailableMail mail = captor.getValue();
        assertThat(mail.staffName()).isEqualTo("김인력");
        assertThat(mail.regionName()).isEqualTo("서울");
        assertThat(mail.round()).isEqualTo(3); // localCourseNumber 우선
        assertThat(mail.date()).isEqualTo(LocalDate.of(2026, 8, 25));
        assertThat(mail.sessionLabel()).isEqualTo("오전");
        assertThat(mail.reason()).isEqualTo("개인 사정");
    }

    @Test
    @DisplayName("이메일 없는/삭제된 사용자는 권한이 있어도 수신 대상에서 제외한다")
    void excludesUsersWithoutEmailOrDeleted() {
        stubCourseAndStaff();
        stubPermittedRecipients(
                user(101L, "관리자A", "a@x.com", false),
                user(104L, "이메일없음", null, false),
                user(105L, "삭제됨", "d@x.com", true));

        service.onStaffBecameUnavailable(event());

        verify(emailService, times(1)).sendStaffUnavailableNotice(anyString(), any());
        verify(emailService).sendStaffUnavailableNotice(eq("a@x.com"), any());
        verify(noticeRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("한 수신자 발송이 실패해도 FAIL 이력을 남기고 나머지 수신자 발송을 계속한다")
    void continuesOnPartialFailure_andRecordsFail() {
        stubCourseAndStaff();
        stubPermittedRecipients(
                user(101L, "관리자A", "fail@x.com", false),
                user(102L, "운영자B", "ok@x.com", false));
        doThrow(new RuntimeException("SMTP down"))
                .when(emailService).sendStaffUnavailableNotice(eq("fail@x.com"), any());

        service.onStaffBecameUnavailable(event());

        // 실패해도 두 번째 수신자 발송 시도됨
        verify(emailService).sendStaffUnavailableNotice(eq("ok@x.com"), any());
        // FAIL/SUCCESS 각각 이력 저장(총 2건)
        ArgumentCaptor<StaffUnavailableNoticeEntity> captor =
                ArgumentCaptor.forClass(StaffUnavailableNoticeEntity.class);
        verify(noticeRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(n -> n.getSendStatus() == NoticeSendStatus.FAIL)
                .anyMatch(n -> n.getSendStatus() == NoticeSendStatus.SUCCESS);
    }

    @Test
    @DisplayName("권한 보유 수신자가 없으면 발송·이력 저장을 하지 않는다")
    void noRecipients_noSendNoHistory() {
        stubCourseAndStaff();
        stubPermittedRecipients();

        service.onStaffBecameUnavailable(event());

        verify(emailService, never()).sendStaffUnavailableNotice(anyString(), any());
        verify(noticeRepository, never()).save(any());
    }

    @Test
    @DisplayName("취소(CANCELED) 회차면 수신자·발송·이력 없이 조기 종료한다")
    void canceledCourse_skipsNotification() {
        CourseStaffEntity cs = CourseStaffEntity.builder()
                .courseStaffId(COURSE_STAFF_ID).courseId(COURSE_ID).userId(STAFF_USER_ID).build();
        when(courseStaffRepository.findById(COURSE_STAFF_ID)).thenReturn(Optional.of(cs));
        CourseEntity canceled = CourseEntity.builder()
                .courseId(COURSE_ID)
                .status(CourseStatus.CANCELED)
                .region(RegionEntity.builder().name("서울").build())
                .localCourseNumber(3)
                .build();
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(canceled));
        stubPermittedRecipients(user(101L, "관리자A", "a@x.com", false));

        service.onStaffBecameUnavailable(event());

        // 취소 회차는 수신자 조회 이전에 조기 종료해야 한다(향후 리팩터 회귀 방지).
        verify(usersRepository, never()).findByCanSendEmailTrue();
        verify(emailService, never()).sendStaffUnavailableNotice(anyString(), any());
        verify(noticeRepository, never()).save(any());
    }
}
