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
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselingType;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CancelCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ChangeCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CompleteCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ContactAttemptResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorAssignment;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorChangedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCanceledResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCompletionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselingSessionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CreateCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.RecordCounselingSessionRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantDetailResponse;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Mock
    private CourseParticipantCounselorRepository courseParticipantCounselorRepository;

    @InjectMocks
    private CourseParticipantServiceImpl service;

    private CourseParticipantEntity entity(Long id, CourseParticipantStatus status, Integer contactAttempt) {
        return CourseParticipantEntity.builder()
                .courseParticipantId(id)
                .courseId(15L)
                .participantId(25L)
                .status(status)
                .contactAttempt(contactAttempt)
                .build();
    }

    private CreateCourseParticipantRequest createRequest(Long counselorId) {
        List<CounselorAssignment> counselors = counselorId == null
                ? null
                : List.of(new CounselorAssignment(counselorId, "PRE_SESSION"));
        return new CreateCourseParticipantRequest(
                15L, 25L, counselors, "워크넷",
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

    @Test
    @DisplayName("초기 상태를 지정해 등록하면 해당 상태(CONFIRMED=선정)로 저장된다")
    void create_withInitialStatus_confirmed() {
        when(courseRepository.existsById(15L)).thenReturn(true);
        when(participantRepository.existsById(25L)).thenReturn(true);
        when(courseParticipantRepository.save(any(CourseParticipantEntity.class)))
                .thenReturn(entity(101L, CourseParticipantStatus.CONFIRMED, 0));

        CourseParticipantCreatedResponse response =
                service.create(createRequest(null), CourseParticipantStatus.CONFIRMED);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        ArgumentCaptor<CourseParticipantEntity> captor = ArgumentCaptor.forClass(CourseParticipantEntity.class);
        verify(courseParticipantRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CourseParticipantStatus.CONFIRMED);
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

    @Test
    @DisplayName("상세 조회 시 참여자 표시정보(matchKey)·지역/회차·유입/자격 필드를 매핑한다")
    void findById_mapsExtendedFields() {
        ParticipantEntity participant = ParticipantEntity.builder()
                .participantId(25L)
                .name("김철수")
                .birthYear(1978)
                .phone("010-5678-1234")
                .matchKey("KCS_1978_1234")
                .build();
        RegionEntity region = RegionEntity.builder().regionId(1L).name("서울").build();
        CourseEntity course = CourseEntity.builder()
                .courseId(15L)
                .courseName("양천5기")
                .courseNumber(5)
                .localCourseNumber(2)
                .region(region)
                .build();
        CourseParticipantEntity cp = CourseParticipantEntity.builder()
                .courseParticipantId(101L)
                .courseId(15L)
                .participantId(25L)
                .status(CourseParticipantStatus.CONFIRMED)
                .contactAttempt(0)
                .inflowType("워크넷")
                .applyDate(LocalDate.of(2026, 8, 1))
                .receptionDate(LocalDate.of(2026, 8, 2))
                .basicEducation("Y")
                .participant(participant)
                .course(course)
                .build();
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(cp));
        when(courseParticipantCounselorRepository.findByCourseParticipantId(101L)).thenReturn(List.of());

        CourseParticipantDetailResponse response = service.findById(101L);

        assertThat(response.matchKey()).isEqualTo("KCS_1978_1234");
        assertThat(response.birthYear()).isEqualTo(1978);
        assertThat(response.phone()).isEqualTo("010-5678-1234");
        assertThat(response.regionName()).isEqualTo("서울");
        assertThat(response.courseNumber()).isEqualTo(5);
        assertThat(response.localCourseNumber()).isEqualTo(2);
        assertThat(response.inflowType()).isEqualTo("워크넷");
        assertThat(response.applyDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.receptionDate()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(response.status()).isEqualTo("CONFIRMED");
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

    @Test
    @DisplayName("상담사 변경 시 user 존재 검증 후 배정을 전체 교체하고 결과를 반환한다")
    void changeCounselor_validatesAndUpdates() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.APPLIED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(usersRepository.findByUserIdAndDeletedFalse(12L)).thenReturn(Optional.of(new UsersEntity()));
        CourseParticipantCounselorEntity savedRow = CourseParticipantCounselorEntity.builder()
                .courseParticipantId(101L)
                .counselorId(12L)
                .status(CounselingType.PRE_SESSION)
                .build();
        when(courseParticipantCounselorRepository.findByCourseParticipantId(101L))
                .thenReturn(List.of(savedRow));

        CounselorChangedResponse response = service.changeCounselor(
                101L, new ChangeCounselorRequest(List.of(new CounselorAssignment(12L, "PRE_SESSION"))));

        assertThat(response.courseParticipantId()).isEqualTo(101L);
        assertThat(response.counselors()).hasSize(1);
        assertThat(response.counselors().get(0).counselorId()).isEqualTo(12L);
        assertThat(response.counselors().get(0).status()).isEqualTo("PRE_SESSION");
        verify(courseParticipantCounselorRepository).deleteByCourseParticipantId(101L);
    }

    @Test
    @DisplayName("상담사 변경 시 사전/사후1/사후2 3개 슬롯을 각각 배정할 수 있다")
    void changeCounselor_threeSlots() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.APPLIED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(usersRepository.findByUserIdAndDeletedFalse(any())).thenReturn(Optional.of(new UsersEntity()));
        when(courseParticipantCounselorRepository.findByCourseParticipantId(101L)).thenReturn(List.of());

        service.changeCounselor(101L, new ChangeCounselorRequest(List.of(
                new CounselorAssignment(12L, "PRE_SESSION"),
                new CounselorAssignment(13L, "POST_SESSION_1"),
                new CounselorAssignment(12L, "POST_SESSION_2"))));

        ArgumentCaptor<List<CourseParticipantCounselorEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(courseParticipantCounselorRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
        assertThat(captor.getValue())
                .extracting(CourseParticipantCounselorEntity::getStatus)
                .containsExactly(
                        CounselingType.PRE_SESSION,
                        CounselingType.POST_SESSION_1,
                        CounselingType.POST_SESSION_2);
    }

    @Test
    @DisplayName("상담사 변경 시 같은 상담 구분을 중복 배정하면 COUNSELING_SLOT_DUPLICATED 예외")
    void changeCounselor_duplicateSlot() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.APPLIED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(usersRepository.findByUserIdAndDeletedFalse(12L)).thenReturn(Optional.of(new UsersEntity()));

        assertThatThrownBy(() -> service.changeCounselor(101L, new ChangeCounselorRequest(List.of(
                new CounselorAssignment(12L, "PRE_SESSION"),
                new CounselorAssignment(13L, "PRE_SESSION")))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUNSELING_SLOT_DUPLICATED);
        verify(courseParticipantCounselorRepository, never()).deleteByCourseParticipantId(any());
    }

    @Test
    @DisplayName("구 상담 구분값(PRE/POST)은 INVALID_STATUS 예외")
    void changeCounselor_legacyValue_invalid() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.APPLIED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.changeCounselor(
                101L, new ChangeCounselorRequest(List.of(new CounselorAssignment(12L, "PRE")))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS);
    }

    private CourseParticipantCounselorEntity counselorRow(Long cpId, CounselingType type) {
        return CourseParticipantCounselorEntity.builder()
                .courseParticipantCounselorId(500L)
                .courseParticipantId(cpId)
                .counselorId(12L)
                .status(type)
                .build();
    }

    @Test
    @DisplayName("상담 세션 기록 시 시작/종료 일시·메모가 저장되고 completed=true를 반환한다")
    void recordCounselingSession_success() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.CONFIRMED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));
        CourseParticipantCounselorEntity row = counselorRow(101L, CounselingType.PRE_SESSION);
        when(courseParticipantCounselorRepository.findByCourseParticipantIdAndStatus(
                101L, CounselingType.PRE_SESSION)).thenReturn(Optional.of(row));

        LocalDateTime start = LocalDateTime.of(2026, 7, 20, 14, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 20, 15, 0);
        CounselingSessionResponse response = service.recordCounselingSession(
                101L, "PRE_SESSION", new RecordCounselingSessionRequest(start, end, "상담 진행 완료"));

        assertThat(response.completed()).isTrue();
        assertThat(response.counselingType()).isEqualTo("PRE_SESSION");
        assertThat(response.startedAt()).isEqualTo(start);
        assertThat(response.endedAt()).isEqualTo(end);
        assertThat(response.memo()).isEqualTo("상담 진행 완료");
        assertThat(row.getCounselingStartedAt()).isEqualTo(start);
        assertThat(row.getCounselingEndedAt()).isEqualTo(end);
        assertThat(row.getCounselingMemo()).isEqualTo("상담 진행 완료");
        verify(courseParticipantCounselorRepository).save(row);
    }

    @Test
    @DisplayName("상담 세션 기록 시 null 필드는 기존값을 유지한다(부분 수정)")
    void recordCounselingSession_partialUpdate_keepsExisting() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.CONFIRMED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));
        CourseParticipantCounselorEntity row = counselorRow(101L, CounselingType.POST_SESSION_1);
        LocalDateTime start = LocalDateTime.of(2026, 7, 20, 14, 0);
        row.setCounselingStartedAt(start);
        row.setCounselingMemo("기존 메모");
        when(courseParticipantCounselorRepository.findByCourseParticipantIdAndStatus(
                101L, CounselingType.POST_SESSION_1)).thenReturn(Optional.of(row));

        LocalDateTime end = LocalDateTime.of(2026, 7, 20, 15, 30);
        CounselingSessionResponse response = service.recordCounselingSession(
                101L, "POST_SESSION_1", new RecordCounselingSessionRequest(null, end, null));

        assertThat(response.startedAt()).isEqualTo(start);
        assertThat(response.endedAt()).isEqualTo(end);
        assertThat(response.memo()).isEqualTo("기존 메모");
        assertThat(response.completed()).isTrue();
    }

    @Test
    @DisplayName("상담 세션 기록 시 슬롯에 배정된 상담사가 없으면 COUNSELING_SLOT_NOT_FOUND 예외")
    void recordCounselingSession_slotNotFound() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.CONFIRMED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(courseParticipantCounselorRepository.findByCourseParticipantIdAndStatus(
                101L, CounselingType.POST_SESSION_2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordCounselingSession(
                101L, "POST_SESSION_2",
                new RecordCounselingSessionRequest(LocalDateTime.of(2026, 7, 20, 14, 0), null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUNSELING_SLOT_NOT_FOUND);
    }

    @Test
    @DisplayName("상담 종료 일시가 시작 일시보다 앞서면 INVALID_COUNSELING_TIME 예외")
    void recordCounselingSession_endBeforeStart() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.CONFIRMED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));
        CourseParticipantCounselorEntity row = counselorRow(101L, CounselingType.PRE_SESSION);
        when(courseParticipantCounselorRepository.findByCourseParticipantIdAndStatus(
                101L, CounselingType.PRE_SESSION)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.recordCounselingSession(
                101L, "PRE_SESSION",
                new RecordCounselingSessionRequest(
                        LocalDateTime.of(2026, 7, 20, 15, 0),
                        LocalDateTime.of(2026, 7, 20, 14, 0),
                        null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_COUNSELING_TIME);
        verify(courseParticipantCounselorRepository, never()).save(any(CourseParticipantCounselorEntity.class));
    }

    @Test
    @DisplayName("시작 일시 없이 종료 일시만 있으면 INVALID_COUNSELING_TIME 예외")
    void recordCounselingSession_endWithoutStart() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.CONFIRMED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));
        CourseParticipantCounselorEntity row = counselorRow(101L, CounselingType.PRE_SESSION);
        when(courseParticipantCounselorRepository.findByCourseParticipantIdAndStatus(
                101L, CounselingType.PRE_SESSION)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.recordCounselingSession(
                101L, "PRE_SESSION",
                new RecordCounselingSessionRequest(null, LocalDateTime.of(2026, 7, 20, 15, 0), null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_COUNSELING_TIME);
    }

    @Test
    @DisplayName("유효하지 않은 상담 구분으로 세션 기록 시 INVALID_STATUS 예외")
    void recordCounselingSession_invalidType() {
        CourseParticipantEntity existing = entity(101L, CourseParticipantStatus.CONFIRMED, 0);
        when(courseParticipantRepository.findById(101L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.recordCounselingSession(
                101L, "PRE",
                new RecordCounselingSessionRequest(LocalDateTime.of(2026, 7, 20, 14, 0), null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS);
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
