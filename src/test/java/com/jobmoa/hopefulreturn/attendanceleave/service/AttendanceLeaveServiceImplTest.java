package com.jobmoa.hopefulreturn.attendanceleave.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.attendance.repository.AttendanceRepository;
import com.jobmoa.hopefulreturn.attendanceleave.entity.AttendanceLeaveEntity;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveDeletedResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveUpdatedResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.RegisterAttendanceLeaveRequest;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.UpdateAttendanceLeaveRequest;
import com.jobmoa.hopefulreturn.attendanceleave.repository.AttendanceLeaveRepository;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import java.time.LocalTime;
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
 *   실행: ./gradlew test --tests "*AttendanceLeaveServiceImplTest"  →  BUILD SUCCESSFUL
 *   결과: 6 tests / 0 failures / 0 errors / 0 skipped  →  전체 통과 ✅
 * ──────────────────────────────────────────────────────────────
 */
@ExtendWith(MockitoExtension.class)
class AttendanceLeaveServiceImplTest {

    @Mock
    private AttendanceLeaveRepository attendanceLeaveRepository;
    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private AttendanceLeaveServiceImpl service;

    private AttendanceLeaveEntity entity(Long id) {
        return AttendanceLeaveEntity.builder()
                .attendanceLeaveId(id)
                .attendanceId(31L)
                .leaveTime(LocalTime.of(14, 30))
                .returnTime(LocalTime.of(15, 20))
                .reason("병원 진료")
                .build();
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("등록 시 출석 검증 후 저장하고 전체 응답을 반환한다")
    void register_success() {
        when(attendanceRepository.existsById(31L)).thenReturn(true);
        when(attendanceLeaveRepository.save(any(AttendanceLeaveEntity.class))).thenReturn(entity(5L));

        AttendanceLeaveResponse response = service.register(
                new RegisterAttendanceLeaveRequest(31L, LocalTime.of(14, 30), LocalTime.of(15, 20), "병원 진료"));

        assertThat(response.attendanceLeaveId()).isEqualTo(5L);
        assertThat(response.attendanceId()).isEqualTo(31L);
        assertThat(response.reason()).isEqualTo("병원 진료");
        ArgumentCaptor<AttendanceLeaveEntity> captor = ArgumentCaptor.forClass(AttendanceLeaveEntity.class);
        verify(attendanceLeaveRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("등록 시 출석이 없으면 ATTENDANCE_NOT_FOUND 예외")
    void register_attendanceNotFound() {
        when(attendanceRepository.existsById(31L)).thenReturn(false);

        assertThatThrownBy(() -> service.register(
                new RegisterAttendanceLeaveRequest(31L, null, null, "사유")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_NOT_FOUND);
        verify(attendanceLeaveRepository, never()).save(any());
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("존재하지 않는 조퇴·외출 상세 조회 시 ATTENDANCE_LEAVE_NOT_FOUND 예외")
    void findById_notFound() {
        when(attendanceLeaveRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_LEAVE_NOT_FOUND);
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("수정 시 시각·사유 반영, 응답에 updatedAt(계산값) 포함")
    void update_appliesChanges() {
        AttendanceLeaveEntity existing = entity(5L);
        when(attendanceLeaveRepository.findById(5L)).thenReturn(Optional.of(existing));

        AttendanceLeaveUpdatedResponse response = service.update(
                5L, new UpdateAttendanceLeaveRequest(LocalTime.of(14, 20), LocalTime.of(15, 15), "병원 진료(시간 수정)"));

        assertThat(response.attendanceLeaveId()).isEqualTo(5L);
        assertThat(response.updatedAt()).isNotNull();
        assertThat(existing.getLeaveTime()).isEqualTo(LocalTime.of(14, 20));
        assertThat(existing.getReason()).isEqualTo("병원 진료(시간 수정)");
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("수정 시 null 필드는 기존 값을 유지한다")
    void update_nullFieldsUnchanged() {
        AttendanceLeaveEntity existing = entity(5L);
        when(attendanceLeaveRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.update(5L, new UpdateAttendanceLeaveRequest(null, null, "사유만 변경"));

        assertThat(existing.getLeaveTime()).isEqualTo(LocalTime.of(14, 30));
        assertThat(existing.getReturnTime()).isEqualTo(LocalTime.of(15, 20));
        assertThat(existing.getReason()).isEqualTo("사유만 변경");
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("삭제 시 하드 삭제(repository.delete)를 호출한다")
    void delete_hardDeletes() {
        AttendanceLeaveEntity existing = entity(5L);
        when(attendanceLeaveRepository.findById(5L)).thenReturn(Optional.of(existing));

        AttendanceLeaveDeletedResponse response = service.delete(5L);

        assertThat(response.deleted()).isTrue();
        verify(attendanceLeaveRepository, times(1)).delete(existing);
    }
}
