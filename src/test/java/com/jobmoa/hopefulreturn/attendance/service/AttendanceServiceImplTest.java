package com.jobmoa.hopefulreturn.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.attendance.entity.AttendanceEntity;
import com.jobmoa.hopefulreturn.attendance.entity.AttendanceStatus;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceDeletedResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceUpdatedResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.BulkAttendanceRequest;
import com.jobmoa.hopefulreturn.attendance.model.dto.BulkAttendanceResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.RegisterAttendanceRequest;
import com.jobmoa.hopefulreturn.attendance.model.dto.UpdateAttendanceRequest;
import com.jobmoa.hopefulreturn.attendance.repository.AttendanceRepository;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private CourseParticipantRepository courseParticipantRepository;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private AttendanceServiceImpl service;

    private AttendanceEntity entity(Long id, AttendanceStatus status) {
        return AttendanceEntity.builder()
                .attendanceId(id)
                .courseParticipantId(15L)
                .dayNo(1)
                .checkInTime(LocalTime.of(8, 55, 23))
                .checkOutTime(LocalTime.of(18, 2, 10))
                .status(status)
                .build();
    }

    private CourseParticipantEntity participantEntity(Long courseParticipantId, Long courseId) {
        return CourseParticipantEntity.builder()
                .courseParticipantId(courseParticipantId)
                .courseId(courseId)
                .build();
    }

    private CourseEntity courseEntity(Long courseId, LocalTime startTime, LocalTime endTime) {
        return CourseEntity.builder()
                .courseId(courseId)
                .educationStartTime(startTime)
                .educationEndTime(endTime)
                .build();
    }

    @Test
    @DisplayName("등록 시 수강 정보 검증 후 저장하고 전체 응답을 반환한다")
    void register_success() {
        when(courseParticipantRepository.existsById(15L)).thenReturn(true);
        when(courseParticipantRepository.findById(15L))
                .thenReturn(Optional.of(participantEntity(15L, 10L)));
        when(courseRepository.findById(10L))
                .thenReturn(Optional.of(courseEntity(10L, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(attendanceRepository.save(any(AttendanceEntity.class)))
                .thenReturn(entity(31L, AttendanceStatus.ATTEND));

        AttendanceResponse response = service.register(
                new RegisterAttendanceRequest(15L, 1, LocalTime.of(8, 55, 23), LocalTime.of(18, 2, 10), null, null));

        assertThat(response.attendanceId()).isEqualTo(31L);
        assertThat(response.status()).isEqualTo("ATTEND");
        ArgumentCaptor<AttendanceEntity> captor = ArgumentCaptor.forClass(AttendanceEntity.class);
        verify(attendanceRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AttendanceStatus.ATTEND);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("등록 시 수강 정보가 없으면 COURSE_PARTICIPANT_NOT_FOUND 예외")
    void register_courseParticipantNotFound() {
        when(courseParticipantRepository.existsById(15L)).thenReturn(false);

        assertThatThrownBy(() -> service.register(
                new RegisterAttendanceRequest(15L, 1, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND);
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("등록 시 강좌에 교육 시작시간이 없으면 COURSE_EDUCATION_START_TIME_NOT_SET 예외")
    void register_courseEducationStartTimeMissing() {
        when(courseParticipantRepository.existsById(15L)).thenReturn(true);
        when(courseParticipantRepository.findById(15L))
                .thenReturn(Optional.of(participantEntity(15L, 10L)));
        when(courseRepository.findById(10L))
                .thenReturn(Optional.of(courseEntity(10L, null, null)));

        assertThatThrownBy(() -> service.register(
                new RegisterAttendanceRequest(15L, 1, LocalTime.of(9, 0), null, null, null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.COURSE_EDUCATION_START_TIME_NOT_SET);
                    assertThat(be.getMessage()).contains("courseId=10", "교육 시작 시간");
                });
    }

    @Test
    @DisplayName("등록 시 강좌에 교육 종료시간이 없으면 COURSE_EDUCATION_END_TIME_NOT_SET 예외")
    void register_courseEducationEndTimeMissing() {
        when(courseParticipantRepository.existsById(15L)).thenReturn(true);
        when(courseParticipantRepository.findById(15L))
                .thenReturn(Optional.of(participantEntity(15L, 10L)));
        when(courseRepository.findById(10L))
                .thenReturn(Optional.of(courseEntity(10L, LocalTime.of(9, 0), null)));

        assertThatThrownBy(() -> service.register(
                new RegisterAttendanceRequest(15L, 1, LocalTime.of(9, 1), null, null, null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.COURSE_EDUCATION_END_TIME_NOT_SET);
                    assertThat(be.getMessage()).contains("courseId=10", "교육 종료 시간");
                });
    }

    @Test
    @DisplayName("일괄 등록 시 강좌·각 수강 정보 검증 후 saveAll, savedCount 반환")
    void registerBulk_success() {
        when(courseRepository.existsById(10L)).thenReturn(true);
        when(courseParticipantRepository.existsById(any())).thenReturn(true);

        when(courseParticipantRepository.findById(101L))
                .thenReturn(Optional.of(participantEntity(101L, 10L)));
        when(courseParticipantRepository.findById(102L))
                .thenReturn(Optional.of(participantEntity(102L, 10L)));
        when(courseParticipantRepository.findById(103L))
                .thenReturn(Optional.of(participantEntity(103L, 10L)));
        when(courseRepository.findById(10L))
                .thenReturn(Optional.of(courseEntity(10L, LocalTime.of(9, 0), LocalTime.of(18, 0))));

        when(attendanceRepository.saveAll(anyList()))
                .thenReturn(List.of(entity(1L, AttendanceStatus.ATTEND), entity(2L, AttendanceStatus.LATE),
                        entity(3L, AttendanceStatus.ABSENT)));

        BulkAttendanceRequest request = new BulkAttendanceRequest(10L, 1, List.of(
                new BulkAttendanceRequest.Item(101L, LocalTime.of(8, 55), LocalTime.of(18, 1)),
                new BulkAttendanceRequest.Item(102L, LocalTime.of(9, 7), LocalTime.of(18, 0)),
                new BulkAttendanceRequest.Item(103L, null, null)));

        BulkAttendanceResponse response = service.registerBulk(request);

        assertThat(response.savedCount()).isEqualTo(3);
        assertThat(response.dayNo()).isEqualTo(1);
        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.message()).isEqualTo("출석 정보가 저장되었습니다.");
    }

    @Test
    @DisplayName("일괄 등록 시 강좌가 없으면 COURSE_NOT_FOUND 예외")
    void registerBulk_courseNotFound() {
        when(courseRepository.existsById(10L)).thenReturn(false);

        BulkAttendanceRequest request = new BulkAttendanceRequest(10L, 1, List.of(
                new BulkAttendanceRequest.Item(101L, null, null)));

        assertThatThrownBy(() -> service.registerBulk(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
        verify(attendanceRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("일괄 등록 시 항목의 수강 정보가 없으면 COURSE_PARTICIPANT_NOT_FOUND 예외")
    void registerBulk_itemCourseParticipantNotFound() {
        when(courseRepository.existsById(10L)).thenReturn(true);
        when(courseParticipantRepository.existsById(101L)).thenReturn(false);

        BulkAttendanceRequest request = new BulkAttendanceRequest(10L, 1, List.of(
                new BulkAttendanceRequest.Item(101L, null, null)));

        assertThatThrownBy(() -> service.registerBulk(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND);
    }

    @Test
    @DisplayName("일괄 등록 시 강좌에 교육 시작시간이 없으면 COURSE_EDUCATION_START_TIME_NOT_SET 예외")
    void registerBulk_courseEducationStartTimeMissing() {
        when(courseRepository.existsById(10L)).thenReturn(true);
        when(courseParticipantRepository.existsById(101L)).thenReturn(true);
        when(courseParticipantRepository.findById(101L))
                .thenReturn(Optional.of(participantEntity(101L, 10L)));
        when(courseRepository.findById(10L))
                .thenReturn(Optional.of(courseEntity(10L, null, LocalTime.of(18, 0))));

        BulkAttendanceRequest request = new BulkAttendanceRequest(10L, 1, List.of(
                new BulkAttendanceRequest.Item(101L, LocalTime.of(9, 0), null)));

        assertThatThrownBy(() -> service.registerBulk(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_EDUCATION_START_TIME_NOT_SET);
        verify(attendanceRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 출석 상세 조회 시 ATTENDANCE_NOT_FOUND 예외")
    void findById_notFound() {
        when(attendanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("수정 시 status 반영, 응답에 updatedAt(계산값) 포함")
    void update_appliesStatus() {
        AttendanceEntity existing = entity(31L, AttendanceStatus.ATTEND);
        when(attendanceRepository.findById(31L)).thenReturn(Optional.of(existing));
        when(courseParticipantRepository.findById(15L))
                .thenReturn(Optional.of(participantEntity(15L, 10L)));
        when(courseRepository.findById(10L))
                .thenReturn(Optional.of(courseEntity(10L, LocalTime.of(9, 0), LocalTime.of(18, 0))));

        AttendanceUpdatedResponse response = service.update(
                31L, new UpdateAttendanceRequest(LocalTime.of(9, 3, 10), LocalTime.of(18, 0), null, null));

        assertThat(response.attendanceId()).isEqualTo(31L);
        assertThat(response.status()).isEqualTo("LATE");
        assertThat(response.updatedAt()).isNotNull();
        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.LATE);
        assertThat(existing.getCheckInTime()).isEqualTo(LocalTime.of(9, 3, 10));
    }

    @Test
    @DisplayName("수정 시 강좌에 교육 시작시간이 없으면 COURSE_EDUCATION_START_TIME_NOT_SET 예외")
    void update_courseEducationStartTimeMissing() {
        AttendanceEntity existing = entity(31L, AttendanceStatus.ATTEND);
        when(attendanceRepository.findById(31L)).thenReturn(Optional.of(existing));
        when(courseParticipantRepository.findById(15L))
                .thenReturn(Optional.of(participantEntity(15L, 10L)));
        when(courseRepository.findById(10L))
                .thenReturn(Optional.of(courseEntity(10L, null, null)));

        assertThatThrownBy(() -> service.update(31L, new UpdateAttendanceRequest(LocalTime.of(9, 0), null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_EDUCATION_START_TIME_NOT_SET);
    }

    @Test
    @DisplayName("삭제 시 하드 삭제(repository.delete)를 호출한다")
    void delete_hardDeletes() {
        AttendanceEntity existing = entity(31L, AttendanceStatus.ATTEND);
        when(attendanceRepository.findById(31L)).thenReturn(Optional.of(existing));

        AttendanceDeletedResponse response = service.delete(31L);

        assertThat(response.deleted()).isTrue();
        verify(attendanceRepository, times(1)).delete(existing);
    }

    @Test
    @DisplayName("수기 결석 등록(absent=true) 시 입·퇴실 시각 없이 상태 ABSENT·사유 저장, 교육시간 조회 없음")
    void register_absent_savesReasonWithoutCourseLookup() {
        when(courseParticipantRepository.existsById(15L)).thenReturn(true);
        when(attendanceRepository.save(any(AttendanceEntity.class)))
                .thenReturn(entity(31L, AttendanceStatus.ABSENT));

        service.register(new RegisterAttendanceRequest(
                15L, 1, LocalTime.of(8, 55), null, true, "개인 사정"));

        ArgumentCaptor<AttendanceEntity> captor = ArgumentCaptor.forClass(AttendanceEntity.class);
        verify(attendanceRepository).save(captor.capture());
        AttendanceEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(saved.getAbsenceReason()).isEqualTo("개인 사정");
        assertThat(saved.getCheckInTime()).isNull();
        assertThat(saved.getCheckOutTime()).isNull();
        // 결석 처리 경로에서는 강좌 교육시간을 조회하지 않는다.
        verify(courseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("수기 결석 수정(absent=true) 시 입·퇴실 시각을 지우고 ABSENT·사유로 갱신")
    void update_absent_clearsTimesAndSavesReason() {
        AttendanceEntity existing = entity(31L, AttendanceStatus.ATTEND);
        when(attendanceRepository.findById(31L)).thenReturn(Optional.of(existing));

        AttendanceUpdatedResponse response = service.update(
                31L, new UpdateAttendanceRequest(null, null, true, "가족 경조사"));

        assertThat(response.status()).isEqualTo("ABSENT");
        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(existing.getCheckInTime()).isNull();
        assertThat(existing.getCheckOutTime()).isNull();
        assertThat(existing.getAbsenceReason()).isEqualTo("가족 경조사");
        verify(courseRepository, never()).findById(any());
    }
}
