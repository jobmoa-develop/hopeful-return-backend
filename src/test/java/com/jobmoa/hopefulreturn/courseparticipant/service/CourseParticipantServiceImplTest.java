package com.jobmoa.hopefulreturn.courseparticipant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CancelCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ChangeCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CompleteCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ContactAttemptResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorChangedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCanceledResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCompletionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CreateCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDate;
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
 *   실행: ./gradlew test --tests "*CourseParticipantServiceImplTest"  →  BUILD SUCCESSFUL
 *   결과: 12 tests / 0 failures / 0 errors / 0 skipped  →  전체 통과 ✅
 *   개별 결과는 각 @Test 위 주석 참고.
 * ──────────────────────────────────────────────────────────────
 */
@ExtendWith(MockitoExtension.class)
class CourseParticipantServiceImplTest {

    @Mock
    private CourseParticipantRepository courseParticipantRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private CourseParticipantServiceImpl service;

    private CourseParticipantEntity entity(Long id, CourseParticipantStatus status, Integer contactAttempt) {
        return CourseParticipantEntity.builder()
                .courseParticipantId(id)
                .courseId(15L)
                .participantId(25L)
                .counselorId(8L)
                .status(status)
                .contactAttempt(contactAttempt)
                .build();
    }

    private CreateCourseParticipantRequest createRequest(Long counselorId) {
        return new CreateCourseParticipantRequest(
                15L, 25L, counselorId, "워크넷",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2), "Y");
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("등록 시 FK를 검증하고 status=APPLIED·contactAttempt=0으로 저장한다")
    void create_success() {
        // Arrange
        when(courseRepository.existsById(15L)).thenReturn(true);
        when(participantRepository.existsById(25L)).thenReturn(true);
        when(usersRepository.findByUserIdAndDeletedFalse(8L))
                .thenReturn(Optional.of(new UsersEntity()));
        when(courseParticipantRepository.save(any(CourseParticipantEntity.class)))
                .thenReturn(entity(101L, CourseParticipantStatus.APPLIED, 0));

        // Act
        CourseParticipantCreatedResponse response = service.create(createRequest(8L));

        // Assert
        assertThat(response.courseParticipantId()).isEqualTo(101L);
        assertThat(response.status()).isEqualTo("APPLIED");
        ArgumentCaptor<CourseParticipantEntity> captor = ArgumentCaptor.forClass(CourseParticipantEntity.class);
        verify(courseParticipantRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CourseParticipantStatus.APPLIED);
        assertThat(captor.getValue().getContactAttempt()).isZero();
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("등록 시 강좌가 없으면 COURSE_NOT_FOUND 예외")
    void create_courseNotFound() {
        when(courseRepository.existsById(15L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(createRequest(8L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
        verify(courseParticipantRepository, never()).save(any());
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("등록 시 참여자가 없으면 PARTICIPANT_NOT_FOUND 예외")
    void create_participantNotFound() {
        when(courseRepository.existsById(15L)).thenReturn(true);
        when(participantRepository.existsById(25L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(createRequest(8L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("등록 시 상담사(user)가 없으면 USER_NOT_FOUND 예외")
    void create_counselorNotFound() {
        when(courseRepository.existsById(15L)).thenReturn(true);
        when(participantRepository.existsById(25L)).thenReturn(true);
        when(usersRepository.findByUserIdAndDeletedFalse(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(createRequest(8L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("counselorId가 없으면 상담사 검증을 건너뛰고 등록된다")
    void create_nullCounselor_skipsUserValidation() {
        when(courseRepository.existsById(15L)).thenReturn(true);
        when(participantRepository.existsById(25L)).thenReturn(true);
        when(courseParticipantRepository.save(any(CourseParticipantEntity.class)))
                .thenReturn(entity(101L, CourseParticipantStatus.APPLIED, 0));

        service.create(createRequest(null));

        verify(usersRepository, never()).findByUserIdAndDeletedFalse(any());
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("존재하지 않는 수강 상세 조회 시 COURSE_PARTICIPANT_NOT_FOUND 예외")
    void findById_notFound() {
        when(courseParticipantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND);
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("취소 시 status=CANCELED, reason은 incompleteReason에 기록된다")
    void cancel_setsCanceledAndReason() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.APPLIED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));

        CourseParticipantCanceledResponse response =
                service.cancel(101L, new CancelCourseParticipantRequest("참여자 요청"));

        assertThat(response.status()).isEqualTo("CANCELED");
        assertThat(existing.getStatus()).isEqualTo(CourseParticipantStatus.CANCELED);
        assertThat(existing.getIncompleteReason()).isEqualTo("참여자 요청");
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("수료 처리 시 COMPLETED 상태와 수료일이 반영된다")
    void complete_completed() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.APPLIED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));

        CourseParticipantCompletionResponse response = service.complete(
                101L,
                new CompleteCourseParticipantRequest("COMPLETED", LocalDate.of(2026, 8, 24), null));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(existing.getStatus()).isEqualTo(CourseParticipantStatus.COMPLETED);
        assertThat(existing.getCompletionDate()).isEqualTo(LocalDate.of(2026, 8, 24));
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("수료 처리 시 COMPLETED/INCOMPLETE 외 상태값은 INVALID_STATUS 예외")
    void complete_invalidStatus() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.APPLIED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.complete(
                101L,
                new CompleteCourseParticipantRequest("APPLIED", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS);
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("연락 시도 시 횟수가 1 증가한다(null이면 1)")
    void increaseContactAttempt_increments() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.APPLIED, null);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));

        ContactAttemptResponse response = service.increaseContactAttempt(101L);

        assertThat(response.courseParticipantId()).isEqualTo(101L);
        assertThat(response.contactAttempt()).isEqualTo(1);
        assertThat(existing.getContactAttempt()).isEqualTo(1);
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("상담사 변경 시 user 존재 검증 후 counselorId를 반영한다")
    void changeCounselor_validatesAndUpdates() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.APPLIED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(usersRepository.findByUserIdAndDeletedFalse(12L)).thenReturn(Optional.of(new UsersEntity()));

        CounselorChangedResponse response = service.changeCounselor(101L, new ChangeCounselorRequest(12L));

        assertThat(response.counselorId()).isEqualTo(12L);
        assertThat(existing.getCounselorId()).isEqualTo(12L);
    }

    // ✅ PASS (2026-07-07)
    @Test
    @DisplayName("삭제 시 하드 삭제(repository.delete)를 호출한다")
    void delete_hardDeletes() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.APPLIED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));

        CourseParticipantDeletedResponse response = service.delete(101L);

        assertThat(response.deleted()).isTrue();
        verify(courseParticipantRepository, times(1)).delete(existing);
    }
}
