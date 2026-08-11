package com.jobmoa.hopefulreturn.attendance.qr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.attendance.entity.AttendanceEntity;
import com.jobmoa.hopefulreturn.attendance.entity.AttendanceStatus;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrLeaveRequest;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrLeaveReturnRequest;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrStatusResponse;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrVerifyRequest;
import com.jobmoa.hopefulreturn.attendance.repository.AttendanceRepository;
import com.jobmoa.hopefulreturn.attendanceleave.entity.AttendanceLeaveEntity;
import com.jobmoa.hopefulreturn.attendanceleave.repository.AttendanceLeaveRepository;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QrAttendanceServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    // 고정 시계: 2026-08-12(수) 09:05 — 아래 회차의 day1Date 와 일치시켜 오늘=1일차로 판정한다.
    private static final Clock FIXED_CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 8, 12, 9, 5, 0, 0, ZONE).toInstant(), ZONE);
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12);

    private static final Long COURSE_ID = 10L;
    private static final Long CP_ID = 100L;
    private static final String NAME = "김철수";
    private static final String LAST4 = "5678";

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseParticipantRepository courseParticipantRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private AttendanceLeaveRepository attendanceLeaveRepository;

    private QrAttendanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new QrAttendanceServiceImpl(
                courseRepository, courseParticipantRepository,
                attendanceRepository, attendanceLeaveRepository, FIXED_CLOCK);
    }

    private CourseEntity course(LocalDate day1, LocalTime start, LocalTime end) {
        return CourseEntity.builder()
                .courseId(COURSE_ID)
                .day1Date(day1)
                .educationStartTime(start)
                .educationEndTime(end)
                .build();
    }

    private CourseParticipantEntity cp() {
        return CourseParticipantEntity.builder()
                .courseParticipantId(CP_ID)
                .courseId(COURSE_ID)
                .participant(ParticipantEntity.builder().name(NAME).phone("010-1234-5678").build())
                .build();
    }

    private AttendanceEntity checkedIn(LocalTime checkIn, LocalTime checkOut) {
        return AttendanceEntity.builder()
                .attendanceId(500L)
                .courseParticipantId(CP_ID)
                .dayNo(1)
                .checkInTime(checkIn)
                .checkOutTime(checkOut)
                .status(AttendanceStatus.ATTEND)
                .build();
    }

    private QrVerifyRequest verifyReq() {
        return new QrVerifyRequest(NAME, LAST4);
    }

    @Test
    @DisplayName("오늘이 교육일이 아니면 QR_NOT_CLASS_DAY 를 던진다")
    void verify_notClassDay() {
        // day1Date 가 어제라 오늘과 매칭되지 않음
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY.minusDays(1), LocalTime.of(9, 0), LocalTime.of(18, 0))));

        assertThatThrownBy(() -> service.verify(COURSE_ID, verifyReq()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.QR_NOT_CLASS_DAY);
    }

    @Test
    @DisplayName("본인확인 매칭이 0명이면 QR_VERIFICATION_FAILED 를 던진다")
    void verify_noMatch() {
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of());

        assertThatThrownBy(() -> service.verify(COURSE_ID, verifyReq()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.QR_VERIFICATION_FAILED);
    }

    @Test
    @DisplayName("본인확인 매칭이 2명 이상(동명이인+동일 뒷자리)이면 QR_VERIFICATION_FAILED 를 던진다")
    void verify_multipleMatch() {
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4))
                .thenReturn(List.of(cp(), cp()));

        assertThatThrownBy(() -> service.verify(COURSE_ID, verifyReq()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.QR_VERIFICATION_FAILED);
    }

    @Test
    @DisplayName("입실이 교육 시작 10분 이내면 ATTEND 로 저장하고 외출을 생성하지 않는다")
    void checkIn_withinGrace_attend() {
        // 시작 09:00, 현재 09:05 → grace(09:10) 이내 → ATTEND
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1)).thenReturn(List.of());
        when(attendanceRepository.save(any(AttendanceEntity.class))).thenReturn(checkedIn(LocalTime.of(9, 5), null));

        service.checkIn(COURSE_ID, verifyReq());

        ArgumentCaptor<AttendanceEntity> captor = ArgumentCaptor.forClass(AttendanceEntity.class);
        verify(attendanceRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AttendanceStatus.ATTEND);
        assertThat(captor.getValue().getCheckInTime()).isEqualTo(LocalTime.of(9, 5));
        verify(attendanceLeaveRepository, never()).save(any());
    }

    @Test
    @DisplayName("입실이 교육 시작 10분을 초과하면 LATE 로 저장하고 [시작~입실] 외출을 자동 생성한다")
    void checkIn_afterGrace_lateWithAutoLeave() {
        // 시작 08:00, 현재 09:05 → grace(08:10) 초과 → LATE + 자동 외출(08:00~09:05)
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(8, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1)).thenReturn(List.of());
        when(attendanceRepository.save(any(AttendanceEntity.class))).thenReturn(checkedIn(LocalTime.of(9, 5), null));

        service.checkIn(COURSE_ID, verifyReq());

        ArgumentCaptor<AttendanceEntity> attCaptor = ArgumentCaptor.forClass(AttendanceEntity.class);
        verify(attendanceRepository).save(attCaptor.capture());
        assertThat(attCaptor.getValue().getStatus()).isEqualTo(AttendanceStatus.LATE);

        ArgumentCaptor<AttendanceLeaveEntity> leaveCaptor = ArgumentCaptor.forClass(AttendanceLeaveEntity.class);
        verify(attendanceLeaveRepository).save(leaveCaptor.capture());
        AttendanceLeaveEntity leave = leaveCaptor.getValue();
        assertThat(leave.getAttendanceId()).isEqualTo(500L);
        assertThat(leave.getLeaveTime()).isEqualTo(LocalTime.of(8, 0));   // 교육 시작
        assertThat(leave.getReturnTime()).isEqualTo(LocalTime.of(9, 5));  // 실제 입실
    }

    @Test
    @DisplayName("이미 입실했으면 QR_ALREADY_CHECKED_IN 을 던진다")
    void checkIn_alreadyCheckedIn() {
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1))
                .thenReturn(List.of(checkedIn(LocalTime.of(9, 0), null)));

        assertThatThrownBy(() -> service.checkIn(COURSE_ID, verifyReq()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.QR_ALREADY_CHECKED_IN);
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("조퇴는 입실 후 leave_time 만 기록한 attendance_leave 를 생성한다")
    void leave_createsLeave() {
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1))
                .thenReturn(List.of(checkedIn(LocalTime.of(9, 0), null)));
        when(attendanceLeaveRepository.findByAttendanceId(500L)).thenReturn(List.of());

        service.leave(COURSE_ID, new QrLeaveRequest(NAME, LAST4, LocalTime.of(14, 30)));

        ArgumentCaptor<AttendanceLeaveEntity> captor = ArgumentCaptor.forClass(AttendanceLeaveEntity.class);
        verify(attendanceLeaveRepository).save(captor.capture());
        assertThat(captor.getValue().getLeaveTime()).isEqualTo(LocalTime.of(14, 30));
        assertThat(captor.getValue().getReturnTime()).isNull();
        assertThat(captor.getValue().getAttendanceId()).isEqualTo(500L);
    }

    @Test
    @DisplayName("복귀는 복귀 미기록 최신 조퇴 건에 return_time 을 채워 외출로 완성한다")
    void leaveReturn_setsReturnTime() {
        AttendanceLeaveEntity openLeave = AttendanceLeaveEntity.builder()
                .attendanceLeaveId(7L).attendanceId(500L).leaveTime(LocalTime.of(14, 30)).build();
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1))
                .thenReturn(List.of(checkedIn(LocalTime.of(9, 0), null)));
        when(attendanceLeaveRepository.findByAttendanceId(500L)).thenReturn(List.of(openLeave));

        service.leaveReturn(COURSE_ID, new QrLeaveReturnRequest(NAME, LAST4, null, LocalTime.of(15, 20)));

        ArgumentCaptor<AttendanceLeaveEntity> captor = ArgumentCaptor.forClass(AttendanceLeaveEntity.class);
        verify(attendanceLeaveRepository).save(captor.capture());
        assertThat(captor.getValue().getAttendanceLeaveId()).isEqualTo(7L);
        assertThat(captor.getValue().getReturnTime()).isEqualTo(LocalTime.of(15, 20));
    }

    @Test
    @DisplayName("조퇴 시각 미지정(버튼 클릭)이면 서버 현재시각으로 leave_time 을 기록한다")
    void leave_nullLeaveTime_usesServerNow() {
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1))
                .thenReturn(List.of(checkedIn(LocalTime.of(9, 0), null)));
        when(attendanceLeaveRepository.findByAttendanceId(500L)).thenReturn(List.of());

        service.leave(COURSE_ID, new QrLeaveRequest(NAME, LAST4, null));

        ArgumentCaptor<AttendanceLeaveEntity> captor = ArgumentCaptor.forClass(AttendanceLeaveEntity.class);
        verify(attendanceLeaveRepository).save(captor.capture());
        assertThat(captor.getValue().getLeaveTime()).isEqualTo(LocalTime.of(9, 5)); // 고정 시계 09:05
        assertThat(captor.getValue().getReturnTime()).isNull();
    }

    @Test
    @DisplayName("복귀 시각 미지정(버튼 클릭)이면 서버 현재시각으로 return_time 을 기록한다")
    void leaveReturn_nullReturnTime_usesServerNow() {
        AttendanceLeaveEntity openLeave = AttendanceLeaveEntity.builder()
                .attendanceLeaveId(7L).attendanceId(500L).leaveTime(LocalTime.of(14, 30)).build();
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1))
                .thenReturn(List.of(checkedIn(LocalTime.of(9, 0), null)));
        when(attendanceLeaveRepository.findByAttendanceId(500L)).thenReturn(List.of(openLeave));

        service.leaveReturn(COURSE_ID, new QrLeaveReturnRequest(NAME, LAST4, null, null));

        ArgumentCaptor<AttendanceLeaveEntity> captor = ArgumentCaptor.forClass(AttendanceLeaveEntity.class);
        verify(attendanceLeaveRepository).save(captor.capture());
        assertThat(captor.getValue().getReturnTime()).isEqualTo(LocalTime.of(9, 5)); // 고정 시계 09:05
    }

    @Test
    @DisplayName("교육 종료 시각 이전 퇴실은 QR_CHECKOUT_BEFORE_END 를 던진다")
    void checkOut_beforeEnd() {
        // 종료 18:00, 현재 09:05 → 종료 전
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1))
                .thenReturn(List.of(checkedIn(LocalTime.of(9, 0), null)));

        assertThatThrownBy(() -> service.checkOut(COURSE_ID, verifyReq()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.QR_CHECKOUT_BEFORE_END);
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("교육 종료 시각 이후 퇴실은 check_out_time 을 기록한다")
    void checkOut_afterEnd() {
        // 종료 09:00, 현재 09:05 → 종료 후
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(9, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1))
                .thenReturn(List.of(checkedIn(LocalTime.of(9, 0), null)));
        when(attendanceLeaveRepository.findByAttendanceId(500L)).thenReturn(List.of());

        QrStatusResponse response = service.checkOut(COURSE_ID, verifyReq());

        ArgumentCaptor<AttendanceEntity> captor = ArgumentCaptor.forClass(AttendanceEntity.class);
        verify(attendanceRepository).save(captor.capture());
        assertThat(captor.getValue().getCheckOutTime()).isEqualTo(LocalTime.of(9, 5));
        assertThat(response.dayNo()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 퇴실했으면 재퇴실은 QR_ALREADY_CHECKED_OUT 을 던진다")
    void checkOut_alreadyCheckedOut() {
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(9, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1))
                .thenReturn(List.of(checkedIn(LocalTime.of(9, 0), LocalTime.of(9, 30))));

        assertThatThrownBy(() -> service.checkOut(COURSE_ID, verifyReq()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.QR_ALREADY_CHECKED_OUT);
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("입실 전 조퇴는 QR_NOT_CHECKED_IN 을 던진다")
    void leave_beforeCheckIn() {
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1)).thenReturn(List.of());

        assertThatThrownBy(() -> service.leave(COURSE_ID, new QrLeaveRequest(NAME, LAST4, LocalTime.of(14, 0))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.QR_NOT_CHECKED_IN);
        verify(attendanceLeaveRepository, never()).save(any());
    }

    @Test
    @DisplayName("복귀 미기록 건이 없으면 복귀 요청은 ATTENDANCE_LEAVE_NOT_FOUND 를 던진다")
    void leaveReturn_noOpenLeave() {
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1))
                .thenReturn(List.of(checkedIn(LocalTime.of(9, 0), null)));
        when(attendanceLeaveRepository.findByAttendanceId(500L)).thenReturn(List.of());

        assertThatThrownBy(() ->
                service.leaveReturn(COURSE_ID, new QrLeaveReturnRequest(NAME, LAST4, null, LocalTime.of(15, 20))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ATTENDANCE_LEAVE_NOT_FOUND);
        verify(attendanceLeaveRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 복귀한 건을 id 로 다시 복귀하면 ATTENDANCE_LEAVE_NOT_FOUND (덮어쓰기 방지)")
    void leaveReturn_alreadyReturnedId() {
        AttendanceLeaveEntity closedLeave = AttendanceLeaveEntity.builder()
                .attendanceLeaveId(7L).attendanceId(500L)
                .leaveTime(LocalTime.of(14, 0)).returnTime(LocalTime.of(15, 0)).build();
        when(courseRepository.findById(COURSE_ID))
                .thenReturn(Optional.of(course(TODAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(courseParticipantRepository.findForQrVerify(COURSE_ID, NAME, LAST4)).thenReturn(List.of(cp()));
        when(attendanceRepository.findByCourseParticipantIdAndDayNo(CP_ID, 1))
                .thenReturn(List.of(checkedIn(LocalTime.of(9, 0), null)));
        when(attendanceLeaveRepository.findByAttendanceId(500L)).thenReturn(List.of(closedLeave));

        assertThatThrownBy(() ->
                service.leaveReturn(COURSE_ID, new QrLeaveReturnRequest(NAME, LAST4, 7L, LocalTime.of(15, 30))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ATTENDANCE_LEAVE_NOT_FOUND);
        verify(attendanceLeaveRepository, never()).save(any());
    }
}
