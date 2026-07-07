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
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
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

/*
 * ── 테스트 결과 요약 (2026-07-07) ──────────────────────────────
 *   실행: ./gradlew test --tests "*AttendanceServiceImplTest"  →  BUILD SUCCESSFUL
 *   결과: 10 tests / 0 failures / 0 errors / 0 skipped  →  전체 통과 ✅
 * ──────────────────────────────────────────────────────────────
 */
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

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("등록 시 수강 정보 검증 후 저장하고 전체 응답을 반환한다")
    void register_success() {
        when(courseParticipantRepository.existsById(15L)).thenReturn(true);
        when(attendanceRepository.save(any(AttendanceEntity.class))).thenReturn(entity(31L, AttendanceStatus.ATTEND));

        AttendanceResponse response = service.register(
                new RegisterAttendanceRequest(15L, 1, LocalTime.of(8, 55, 23), LocalTime.of(18, 2, 10), "ATTEND"));

        assertThat(response.attendanceId()).isEqualTo(31L);
        assertThat(response.status()).isEqualTo("ATTEND");
        ArgumentCaptor<AttendanceEntity> captor = ArgumentCaptor.forClass(AttendanceEntity.class);
        verify(attendanceRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AttendanceStatus.ATTEND);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("등록 시 수강 정보가 없으면 COURSE_PARTICIPANT_NOT_FOUND 예외")
    void register_courseParticipantNotFound() {
        when(courseParticipantRepository.existsById(15L)).thenReturn(false);

        assertThatThrownBy(() -> service.register(
                new RegisterAttendanceRequest(15L, 1, null, null, "ATTEND")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND);
        verify(attendanceRepository, never()).save(any());
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("등록 시 잘못된 status 값은 INVALID_STATUS 예외")
    void register_invalidStatus() {
        when(courseParticipantRepository.existsById(15L)).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterAttendanceRequest(15L, 1, null, null, "PRESENT")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS);
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("일괄 등록 시 강좌·각 수강 정보 검증 후 saveAll, savedCount 반환")
    void registerBulk_success() {
        when(courseRepository.existsById(10L)).thenReturn(true);
        when(courseParticipantRepository.existsById(any())).thenReturn(true);
        when(attendanceRepository.saveAll(anyList()))
                .thenReturn(List.of(entity(1L, AttendanceStatus.ATTEND), entity(2L, AttendanceStatus.LATE),
                        entity(3L, AttendanceStatus.ABSENT)));

        BulkAttendanceRequest request = new BulkAttendanceRequest(10L, 1, List.of(
                new BulkAttendanceRequest.Item(101L, LocalTime.of(8, 55), LocalTime.of(18, 1), "ATTEND"),
                new BulkAttendanceRequest.Item(102L, LocalTime.of(9, 7), LocalTime.of(18, 0), "LATE"),
                new BulkAttendanceRequest.Item(103L, null, null, "ABSENT")));

        BulkAttendanceResponse response = service.registerBulk(request);

        assertThat(response.savedCount()).isEqualTo(3);
        assertThat(response.dayNo()).isEqualTo(1);
        assertThat(response.courseId()).isEqualTo(10L);
        assertThat(response.message()).isEqualTo("출석 정보가 저장되었습니다.");
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("일괄 등록 시 강좌가 없으면 COURSE_NOT_FOUND 예외")
    void registerBulk_courseNotFound() {
        when(courseRepository.existsById(10L)).thenReturn(false);

        BulkAttendanceRequest request = new BulkAttendanceRequest(10L, 1, List.of(
                new BulkAttendanceRequest.Item(101L, null, null, "ATTEND")));

        assertThatThrownBy(() -> service.registerBulk(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
        verify(attendanceRepository, never()).saveAll(anyList());
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("일괄 등록 시 항목의 수강 정보가 없으면 COURSE_PARTICIPANT_NOT_FOUND 예외")
    void registerBulk_itemCourseParticipantNotFound() {
        when(courseRepository.existsById(10L)).thenReturn(true);
        when(courseParticipantRepository.existsById(101L)).thenReturn(false);

        BulkAttendanceRequest request = new BulkAttendanceRequest(10L, 1, List.of(
                new BulkAttendanceRequest.Item(101L, null, null, "ATTEND")));

        assertThatThrownBy(() -> service.registerBulk(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND);
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("존재하지 않는 출석 상세 조회 시 ATTENDANCE_NOT_FOUND 예외")
    void findById_notFound() {
        when(attendanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_NOT_FOUND);
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("수정 시 status 반영, 응답에 updatedAt(계산값) 포함")
    void update_appliesStatus() {
        AttendanceEntity existing = entity(31L, AttendanceStatus.ATTEND);
        when(attendanceRepository.findById(31L)).thenReturn(Optional.of(existing));

        AttendanceUpdatedResponse response = service.update(
                31L, new UpdateAttendanceRequest(LocalTime.of(9, 3, 10), LocalTime.of(18, 0), "LATE"));

        assertThat(response.attendanceId()).isEqualTo(31L);
        assertThat(response.status()).isEqualTo("LATE");
        assertThat(response.updatedAt()).isNotNull();
        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.LATE);
        assertThat(existing.getCheckInTime()).isEqualTo(LocalTime.of(9, 3, 10));
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("수정 시 잘못된 status 값은 INVALID_STATUS 예외")
    void update_invalidStatus() {
        AttendanceEntity existing = entity(31L, AttendanceStatus.ATTEND);
        when(attendanceRepository.findById(31L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(31L, new UpdateAttendanceRequest(null, null, "NOPE")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS);
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("삭제 시 하드 삭제(repository.delete)를 호출한다")
    void delete_hardDeletes() {
        AttendanceEntity existing = entity(31L, AttendanceStatus.ATTEND);
        when(attendanceRepository.findById(31L)).thenReturn(Optional.of(existing));

        AttendanceDeletedResponse response = service.delete(31L);

        assertThat(response.deleted()).isTrue();
        verify(attendanceRepository, times(1)).delete(existing);
    }
}
