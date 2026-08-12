package com.jobmoa.hopefulreturn.coursestaffsms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.StaffRole;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.CourseStaffSmsEntity;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffNotifyType;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffSmsSendStatus;
import com.jobmoa.hopefulreturn.coursestaffsms.model.dto.SendCourseStaffSmsRequest;
import com.jobmoa.hopefulreturn.coursestaffsms.model.dto.SendCourseStaffSmsResponse;
import com.jobmoa.hopefulreturn.coursestaffsms.repository.CourseStaffSmsRepository;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.region.support.RegionResolver;
import com.jobmoa.hopefulreturn.sms.SmsSendCommand;
import com.jobmoa.hopefulreturn.sms.SmsSendResult;
import com.jobmoa.hopefulreturn.sms.SmsService;
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

/**
 * 강좌 담당자 인력배정 안내 문자 발송 서비스 단위 테스트 —
 * {region}/{round}/{role} 치환·SMS/LMS 판별·전화번호 없는 인원 skip·그룹별 이력 저장 검증.
 */
@ExtendWith(MockitoExtension.class)
class CourseStaffSmsServiceImplTest {

    @Mock
    private CourseStaffSmsRepository courseStaffSmsRepository;
    @Mock
    private RegionResolver regionResolver;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private CourseStaffRepository courseStaffRepository;
    @Mock
    private SmsService smsService;

    @InjectMocks
    private CourseStaffSmsServiceImpl service;

    private static final long COURSE_ID = 100L;
    private static final long SENDER_ID = 9L;

    private CourseEntity course(String regionName, Integer localRound) {
        CourseEntity.CourseEntityBuilder builder = CourseEntity.builder().localCourseNumber(localRound);
        if (regionName != null) {
            builder.region(RegionEntity.builder().name(regionName).build());
        }
        return builder.build();
    }

    private UsersEntity user(long id, String name, String phone) {
        return UsersEntity.builder().userId(id).name(name).phone(phone).build();
    }

    private CourseStaffEntity staff(long userId, StaffRole role) {
        return CourseStaffEntity.builder().courseId(COURSE_ID).userId(userId).staffRole(role).build();
    }

    private SendCourseStaffSmsRequest.Group group(StaffNotifyType type, String content, Long... userIds) {
        List<SendCourseStaffSmsRequest.Recipient> recipients = java.util.Arrays.stream(userIds)
                .map(id -> new SendCourseStaffSmsRequest.Recipient(id, null))
                .toList();
        return new SendCourseStaffSmsRequest.Group(type, content, recipients);
    }

    @Test
    @DisplayName("발송: {region}/{round}/{role} 을 수신자별로 치환하고 SMS 로 판별한다")
    void send_substitutesTokensAndDetectsSms() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course("양천", 5)));
        when(courseStaffRepository.findByCourseId(COURSE_ID))
                .thenReturn(List.of(staff(1L, StaffRole.PROJECT_MANAGER)));
        when(usersRepository.findAllById(any())).thenReturn(List.of(user(1L, "홍길동", "01011112222")));
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));

        SendCourseStaffSmsRequest request = new SendCourseStaffSmsRequest(COURSE_ID, List.of(
                group(StaffNotifyType.ASSIGN_NEW, "[잡모아]\n{region} {round}회차 {role}로 배정되었습니다", 1L)));
        SendCourseStaffSmsResponse response = service.send(SENDER_ID, request);

        ArgumentCaptor<SmsSendCommand> captor = ArgumentCaptor.forClass(SmsSendCommand.class);
        verify(smsService).send(captor.capture());
        SmsSendCommand command = captor.getValue();
        assertThat(command.type()).isEqualTo("SMS");
        assertThat(command.recipients()).extracting(SmsSendCommand.Recipient::content)
                .containsExactly("[잡모아]\n양천 5회차 PM로 배정되었습니다");
        assertThat(command.recipients()).extracting(SmsSendCommand.Recipient::to)
                .containsExactly("01011112222");
        assertThat(response.messageFormat()).isEqualTo("SMS");
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("발송: {startDate} 를 개강일(day1) M/d 로 치환한다")
    void send_substitutesStartDate() {
        CourseEntity c = CourseEntity.builder()
                .localCourseNumber(3)
                .region(RegionEntity.builder().name("강서").build())
                .day1Date(LocalDate.of(2026, 8, 18))
                .build();
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(c));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(usersRepository.findAllById(any())).thenReturn(List.of(user(1L, "홍길동", "01011112222")));
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));

        service.send(SENDER_ID, new SendCourseStaffSmsRequest(COURSE_ID, List.of(
                group(StaffNotifyType.ASSIGN_CHANGED, "[잡모아]\n{region} {round}회차({startDate}~) 인력변동", 1L))));

        ArgumentCaptor<SmsSendCommand> captor = ArgumentCaptor.forClass(SmsSendCommand.class);
        verify(smsService).send(captor.capture());
        assertThat(captor.getValue().recipients()).extracting(SmsSendCommand.Recipient::content)
                .containsExactly("[잡모아]\n강서 3회차(8/18~) 인력변동");
    }

    @Test
    @DisplayName("발송: 90바이트 초과 본문은 LMS 로 판별한다")
    void send_detectsLmsWhenOver90Bytes() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course("양천", 5)));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(usersRepository.findAllById(any())).thenReturn(List.of(user(1L, "홍길동", "01011112222")));
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));

        String longContent = "가".repeat(60); // EUC-KR 120바이트 > 90
        SendCourseStaffSmsResponse response = service.send(SENDER_ID, new SendCourseStaffSmsRequest(
                COURSE_ID, List.of(group(StaffNotifyType.ASSIGN_CHANGED, longContent, 1L))));

        assertThat(response.messageFormat()).isEqualTo("LMS");
        ArgumentCaptor<SmsSendCommand> captor = ArgumentCaptor.forClass(SmsSendCommand.class);
        verify(smsService).send(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("LMS");
    }

    @Test
    @DisplayName("발송: 전화번호 없는 인원은 발송에서 제외하고 skipped 로 반환한다")
    void send_skipsRecipientWithoutPhone() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course("양천", 5)));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(usersRepository.findAllById(any())).thenReturn(List.of(
                user(1L, "홍길동", "01011112222"),
                user(2L, "김철수", null))); // 전화번호 없음
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));

        SendCourseStaffSmsResponse response = service.send(SENDER_ID, new SendCourseStaffSmsRequest(
                COURSE_ID, List.of(group(StaffNotifyType.ASSIGN_CHANGED, "인력변동", 1L, 2L))));

        assertThat(response.skipped()).extracting(SendCourseStaffSmsResponse.Skipped::userId).containsExactly(2L);
        assertThat(response.skipped()).extracting(SendCourseStaffSmsResponse.Skipped::name).containsExactly("김철수");
        // 발송·이력은 전화번호 있는 1명만
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.successCount()).isEqualTo(1);
        verify(courseStaffSmsRepository, times(1)).save(any());
        ArgumentCaptor<SmsSendCommand> captor = ArgumentCaptor.forClass(SmsSendCommand.class);
        verify(smsService).send(captor.capture());
        assertThat(captor.getValue().recipients()).hasSize(1);
    }

    @Test
    @DisplayName("발송: phoneOverride 가 있으면 users.phone 대신 그 번호로 발송한다")
    void send_usesPhoneOverride() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course("양천", 5)));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(usersRepository.findAllById(any())).thenReturn(List.of(user(1L, "홍길동", "01011112222")));
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));

        SendCourseStaffSmsRequest.Group group = new SendCourseStaffSmsRequest.Group(
                StaffNotifyType.ASSIGN_NEW, "배정 안내",
                List.of(new SendCourseStaffSmsRequest.Recipient(1L, "01099998888")));
        service.send(SENDER_ID, new SendCourseStaffSmsRequest(COURSE_ID, List.of(group)));

        ArgumentCaptor<SmsSendCommand> captor = ArgumentCaptor.forClass(SmsSendCommand.class);
        verify(smsService).send(captor.capture());
        assertThat(captor.getValue().recipients()).extracting(SmsSendCommand.Recipient::to)
                .containsExactly("01099998888");
    }

    @Test
    @DisplayName("발송: SENS 발송 실패 시 롤백하지 않고 FAIL 로 저장한다")
    void send_savesFailWhenSendFails() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course("양천", 5)));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(usersRepository.findAllById(any())).thenReturn(List.of(user(1L, "홍길동", "01011112222")));
        when(smsService.send(any())).thenThrow(new BusinessException(ErrorCode.SMS_SEND_FAILED));

        SendCourseStaffSmsResponse response = service.send(SENDER_ID, new SendCourseStaffSmsRequest(
                COURSE_ID, List.of(group(StaffNotifyType.ASSIGN_REMOVED, "인력 제외", 1L))));

        ArgumentCaptor<CourseStaffSmsEntity> captor = ArgumentCaptor.forClass(CourseStaffSmsEntity.class);
        verify(courseStaffSmsRepository).save(captor.capture());
        assertThat(captor.getValue().getSendStatus()).isEqualTo(StaffSmsSendStatus.FAIL);
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.successCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("발송: 그룹별로 notify_type 을 나눠 이력을 저장한다(변동+제외)")
    void send_savesPerGroupNotifyType() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course("양천", 5)));
        when(courseStaffRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());
        when(usersRepository.findAllById(any())).thenReturn(List.of(
                user(1L, "홍길동", "01011112222"),
                user(2L, "김철수", "01033334444")));
        when(smsService.send(any())).thenReturn(SmsSendResult.ok("202", "success", "req-1", List.of()));

        SendCourseStaffSmsRequest request = new SendCourseStaffSmsRequest(COURSE_ID, List.of(
                group(StaffNotifyType.ASSIGN_CHANGED, "인력변동", 1L),
                group(StaffNotifyType.ASSIGN_REMOVED, "인력 제외", 2L)));
        service.send(SENDER_ID, request);

        // 그룹마다 1회씩 send, 총 2건 저장 — notify_type 이 그룹별로 다르게 기록
        verify(smsService, times(2)).send(any());
        ArgumentCaptor<CourseStaffSmsEntity> captor = ArgumentCaptor.forClass(CourseStaffSmsEntity.class);
        verify(courseStaffSmsRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(CourseStaffSmsEntity::getNotifyType)
                .containsExactlyInAnyOrder(StaffNotifyType.ASSIGN_CHANGED, StaffNotifyType.ASSIGN_REMOVED);
        assertThat(captor.getAllValues()).allSatisfy(row -> {
            assertThat(row.getSentBy()).isEqualTo(SENDER_ID);
            assertThat(row.getCourseId()).isEqualTo(COURSE_ID);
            assertThat(row.getSentAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("발송: 존재하지 않는 강좌면 COURSE_NOT_FOUND 로 거부한다")
    void send_throwsWhenCourseNotFound() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send(SENDER_ID, new SendCourseStaffSmsRequest(
                COURSE_ID, List.of(group(StaffNotifyType.ASSIGN_NEW, "배정", 1L)))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COURSE_NOT_FOUND);
        verify(smsService, never()).send(any());
        verify(courseStaffSmsRepository, never()).save(any());
    }
}
