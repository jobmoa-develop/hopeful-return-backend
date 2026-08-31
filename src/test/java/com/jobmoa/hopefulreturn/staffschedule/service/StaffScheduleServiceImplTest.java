package com.jobmoa.hopefulreturn.staffschedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.coursestaff.entity.StaffRole;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.staffschedule.entity.StaffScheduleEntity;
import com.jobmoa.hopefulreturn.staffschedule.event.StaffBecameUnavailableEvent;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.BulkStaffScheduleRequest;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.BulkStaffScheduleResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.CreateStaffScheduleRequest;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.StaffScheduleResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.UpdateStaffScheduleRequest;
import com.jobmoa.hopefulreturn.staffschedule.repository.StaffScheduleRepository;
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
import org.springframework.context.ApplicationEventPublisher;

/**
 * 스태프 일정 서비스 단위 테스트. 리포지토리를 목킹해 등록/중복/타인등록 권한/일괄 스킵/
 * 소유권 검증/미존재 예외 등 핵심 비즈니스 규칙을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class StaffScheduleServiceImplTest {

    private static final Long OWNER_ID = 6L;
    private static final Long OTHER_ID = 9L;

    @Mock
    private StaffScheduleRepository staffScheduleRepository;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private CourseStaffRepository courseStaffRepository;

    @InjectMocks
    private StaffScheduleServiceImpl service;

    private StaffScheduleEntity entity(Long id, Long userId, SessionType sessionType) {
        return StaffScheduleEntity.builder()
                .staffScheduleId(id)
                .userId(userId)
                .scheduleDate(LocalDate.of(2026, 8, 18))
                .sessionType(sessionType)
                .isAvailable(true)
                .memo("오전만 가능")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 배정 연결(course_staff_id)이 있는 가용 상태의 배정 행
    private StaffScheduleEntity assignedEntity(Long id, Long userId, Long courseStaffId) {
        StaffScheduleEntity e = entity(id, userId, SessionType.AM);
        e.setCourseStaffId(courseStaffId);
        return e;
    }

    @Test
    @DisplayName("등록 시 사용자 존재·중복 검증 후 저장하고 응답을 반환한다")
    void create_success() {
        when(usersRepository.findByUserIdAndDeletedFalse(OWNER_ID))
                .thenReturn(Optional.of(new com.jobmoa.hopefulreturn.users.entity.UsersEntity()));
        when(staffScheduleRepository.existsByUserIdAndScheduleDateAndSessionType(OWNER_ID, LocalDate.of(2026, 8, 18), SessionType.AM))
                .thenReturn(false);
        when(staffScheduleRepository.save(any(StaffScheduleEntity.class)))
                .thenReturn(entity(12L, OWNER_ID, SessionType.AM));

        StaffScheduleResponse response = service.create(OWNER_ID, false,
                new CreateStaffScheduleRequest(null, LocalDate.of(2026, 8, 18), "AM", null, "오전만 가능"));

        assertThat(response.staffScheduleId()).isEqualTo(12L);
        assertThat(response.userId()).isEqualTo(OWNER_ID);
        assertThat(response.sessionType()).isEqualTo("AM");
        ArgumentCaptor<StaffScheduleEntity> captor = ArgumentCaptor.forClass(StaffScheduleEntity.class);
        verify(staffScheduleRepository).save(captor.capture());
        // isAvailable 생략 시 기본 true, createdAt 세팅
        assertThat(captor.getValue().getIsAvailable()).isTrue();
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(captor.getValue().getUserId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("등록 시 중복(user·date·session)이면 DUPLICATE_STAFF_SCHEDULE 예외")
    void create_duplicate() {
        when(usersRepository.findByUserIdAndDeletedFalse(OWNER_ID))
                .thenReturn(Optional.of(new com.jobmoa.hopefulreturn.users.entity.UsersEntity()));
        when(staffScheduleRepository.existsByUserIdAndScheduleDateAndSessionType(OWNER_ID, LocalDate.of(2026, 8, 18), SessionType.AM))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(OWNER_ID, false,
                new CreateStaffScheduleRequest(null, LocalDate.of(2026, 8, 18), "AM", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_STAFF_SCHEDULE);
        verify(staffScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("타인 등록은 관리자가 아니면 ACCESS_DENIED 예외")
    void create_forOther_notManager_denied() {
        assertThatThrownBy(() -> service.create(OWNER_ID, false,
                new CreateStaffScheduleRequest(OTHER_ID, LocalDate.of(2026, 8, 18), "AM", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(staffScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("등록 시 잘못된 sessionType 이면 INVALID_SESSION_TYPE 예외")
    void create_invalidSessionType() {
        when(usersRepository.findByUserIdAndDeletedFalse(OWNER_ID))
                .thenReturn(Optional.of(new com.jobmoa.hopefulreturn.users.entity.UsersEntity()));

        assertThatThrownBy(() -> service.create(OWNER_ID, false,
                new CreateStaffScheduleRequest(null, LocalDate.of(2026, 8, 18), "EVENING", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SESSION_TYPE);
    }

    @Test
    @DisplayName("등록 시 대상 사용자가 없으면 USER_NOT_FOUND 예외")
    void create_userNotFound() {
        when(usersRepository.findByUserIdAndDeletedFalse(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(OWNER_ID, false,
                new CreateStaffScheduleRequest(null, LocalDate.of(2026, 8, 18), "AM", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("일괄 등록은 이미 존재하는 항목을 스킵하고 등록/스킵 건수를 반환한다")
    void createBulk_skipsDuplicates() {
        when(usersRepository.findByUserIdAndDeletedFalse(OWNER_ID))
                .thenReturn(Optional.of(new com.jobmoa.hopefulreturn.users.entity.UsersEntity()));
        // 8/18 FULL 은 이미 존재(스킵), 8/19 AM 은 신규(등록)
        when(staffScheduleRepository.existsByUserIdAndScheduleDateAndSessionType(OWNER_ID, LocalDate.of(2026, 8, 18), SessionType.FULL))
                .thenReturn(true);
        when(staffScheduleRepository.existsByUserIdAndScheduleDateAndSessionType(OWNER_ID, LocalDate.of(2026, 8, 19), SessionType.AM))
                .thenReturn(false);
        when(staffScheduleRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        BulkStaffScheduleResponse response = service.createBulk(OWNER_ID, false,
                new BulkStaffScheduleRequest(null, List.of(
                        new BulkStaffScheduleRequest.Entry(LocalDate.of(2026, 8, 18), "FULL", null, null),
                        new BulkStaffScheduleRequest.Entry(LocalDate.of(2026, 8, 19), "AM", null, "오후 회의"))));

        assertThat(response.registered()).isEqualTo(1);
        assertThat(response.skipped()).isEqualTo(1);
        ArgumentCaptor<List<StaffScheduleEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(staffScheduleRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getSessionType()).isEqualTo(SessionType.AM);
    }

    @Test
    @DisplayName("수정 시 소유자가 아니고 관리자도 아니면 ACCESS_DENIED 예외")
    void update_notOwner_notManager_denied() {
        when(staffScheduleRepository.findById(12L)).thenReturn(Optional.of(entity(12L, OWNER_ID, SessionType.AM)));

        assertThatThrownBy(() -> service.update(12L, OTHER_ID, false,
                new UpdateStaffScheduleRequest("PM", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(staffScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정 시 관리자는 소유자가 아니어도 sessionType·memo 를 변경한다")
    void update_manager_success() {
        StaffScheduleEntity found = entity(12L, OWNER_ID, SessionType.AM);
        when(staffScheduleRepository.findById(12L)).thenReturn(Optional.of(found));
        when(staffScheduleRepository.save(any(StaffScheduleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffScheduleResponse response = service.update(12L, OTHER_ID, true,
                new UpdateStaffScheduleRequest("PM", false, "오후로 변경"));

        assertThat(response.sessionType()).isEqualTo("PM");
        assertThat(response.isAvailable()).isFalse();
        assertThat(response.memo()).isEqualTo("오후로 변경");
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("수정 대상이 없으면 STAFF_SCHEDULE_NOT_FOUND 예외")
    void update_notFound() {
        when(staffScheduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, OWNER_ID, false,
                new UpdateStaffScheduleRequest("PM", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STAFF_SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제 시 소유자는 삭제에 성공한다(미배정 행은 알림 이벤트 없음)")
    void delete_owner_success() {
        StaffScheduleEntity found = entity(12L, OWNER_ID, SessionType.AM); // courseStaffId=null
        when(staffScheduleRepository.findById(12L)).thenReturn(Optional.of(found));

        service.delete(12L, OWNER_ID, false, null);

        verify(staffScheduleRepository).delete(found);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("삭제 시 소유자가 아니고 관리자도 아니면 ACCESS_DENIED 예외")
    void delete_notOwner_denied() {
        when(staffScheduleRepository.findById(12L)).thenReturn(Optional.of(entity(12L, OWNER_ID, SessionType.AM)));

        assertThatThrownBy(() -> service.delete(12L, OTHER_ID, false, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(staffScheduleRepository, never()).delete(any());
    }

    @Test
    @DisplayName("배정된 날짜(course_staff_id 有) 삭제 시 사유(reason)를 담아 재배정 알림 이벤트를 발행한다")
    void delete_assigned_publishesEventWithReason() {
        StaffScheduleEntity found = assignedEntity(12L, OWNER_ID, 77L);
        when(staffScheduleRepository.findById(12L)).thenReturn(Optional.of(found));

        service.delete(12L, OWNER_ID, false, "병원 예약");

        verify(staffScheduleRepository).delete(found);
        ArgumentCaptor<StaffBecameUnavailableEvent> captor =
                ArgumentCaptor.forClass(StaffBecameUnavailableEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        StaffBecameUnavailableEvent event = captor.getValue();
        assertThat(event.staffScheduleId()).isEqualTo(12L);
        assertThat(event.courseStaffId()).isEqualTo(77L);
        assertThat(event.userId()).isEqualTo(OWNER_ID);
        assertThat(event.memo()).isEqualTo("병원 예약");
    }

    @Test
    @DisplayName("배정된 날짜(course_staff_id 有)를 가능→불가로 바꾸면 인력배정에서 해제(course_staff_id=null)하고 원래 배정 ID 로 근무불가 이벤트를 발행한다")
    void update_availableToUnavailable_assigned_dropsAssignmentAndPublishesEvent() {
        StaffScheduleEntity found = assignedEntity(12L, OWNER_ID, 77L);
        when(staffScheduleRepository.findById(12L)).thenReturn(Optional.of(found));
        when(staffScheduleRepository.save(any(StaffScheduleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(12L, OWNER_ID, false, new UpdateStaffScheduleRequest(null, false, "개인 사정"));

        // 삭제와 동일하게 인력배정에서 빠지도록 course_staff_id 연결을 해제하고, 불가 표식은 유지한다.
        assertThat(found.getCourseStaffId()).isNull();
        assertThat(found.getIsAvailable()).isFalse();

        ArgumentCaptor<StaffBecameUnavailableEvent> captor =
                ArgumentCaptor.forClass(StaffBecameUnavailableEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        StaffBecameUnavailableEvent event = captor.getValue();
        assertThat(event.staffScheduleId()).isEqualTo(12L);
        // 알림 이벤트는 해제 전 원래 배정 ID(77)를 담아야 한다(엔티티는 이미 null 이므로 캡처값 사용).
        assertThat(event.courseStaffId()).isEqualTo(77L);
        assertThat(event.userId()).isEqualTo(OWNER_ID);
        assertThat(event.scheduleDate()).isEqualTo(LocalDate.of(2026, 8, 18));
        assertThat(event.sessionType()).isEqualTo(SessionType.AM);
        assertThat(event.memo()).isEqualTo("개인 사정");
    }

    @Test
    @DisplayName("배정 연결이 없는(course_staff_id null) 순수 불가일 전환은 이벤트를 발행하지 않는다")
    void update_availableToUnavailable_notAssigned_noEvent() {
        StaffScheduleEntity found = entity(12L, OWNER_ID, SessionType.AM); // courseStaffId=null
        when(staffScheduleRepository.findById(12L)).thenReturn(Optional.of(found));
        when(staffScheduleRepository.save(any(StaffScheduleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(12L, OWNER_ID, false, new UpdateStaffScheduleRequest(null, false, null));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("불가로의 전이가 아니면(그대로 가능) 이벤트를 발행하지 않는다")
    void update_stillAvailable_noEvent() {
        StaffScheduleEntity found = assignedEntity(12L, OWNER_ID, 77L);
        when(staffScheduleRepository.findById(12L)).thenReturn(Optional.of(found));
        when(staffScheduleRepository.save(any(StaffScheduleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(12L, OWNER_ID, false, new UpdateStaffScheduleRequest(null, true, "여전히 가능"));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("isAvailable 미지정(세션·memo만 변경)이면 이벤트를 발행하지 않는다")
    void update_availabilityUnchanged_noEvent() {
        StaffScheduleEntity found = assignedEntity(12L, OWNER_ID, 77L);
        when(staffScheduleRepository.findById(12L)).thenReturn(Optional.of(found));
        when(staffScheduleRepository.save(any(StaffScheduleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(12L, OWNER_ID, false, new UpdateStaffScheduleRequest("PM", null, "메모만 변경"));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("배정된 일정 조회 시 courseName(지역+회차)·courseStatus·courseStaffRole(배정 역할) 을 채워 반환한다")
    void findById_assigned_populatesCourseNameAndStatus() {
        StaffScheduleEntity found = assignedEntity(12L, OWNER_ID, 77L);
        when(staffScheduleRepository.findById(12L)).thenReturn(Optional.of(found));
        CourseStaffEntity cs = CourseStaffEntity.builder()
                .courseStaffId(77L)
                .staffRole(StaffRole.LECTURER)
                .course(CourseEntity.builder()
                        .localCourseNumber(3)
                        .status(CourseStatus.IN_PROGRESS)
                        .region(RegionEntity.builder().name("서울").build())
                        .build())
                .build();
        when(courseStaffRepository.findWithCourseAndRegionByIdIn(List.of(77L))).thenReturn(List.of(cs));

        StaffScheduleResponse response = service.findById(12L);

        assertThat(response.courseStaffId()).isEqualTo(77L);
        assertThat(response.courseName()).isEqualTo("서울 3회차");
        assertThat(response.courseStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.courseStaffRole()).isEqualTo("LECTURER");
    }

    @Test
    @DisplayName("미배정 일정 조회 시 courseName·courseStatus·courseStaffRole 은 null 이고 배정 조회를 하지 않는다")
    void findById_unassigned_noCourseNameLookup() {
        StaffScheduleEntity found = entity(12L, OWNER_ID, SessionType.AM); // courseStaffId=null
        when(staffScheduleRepository.findById(12L)).thenReturn(Optional.of(found));

        StaffScheduleResponse response = service.findById(12L);

        assertThat(response.courseName()).isNull();
        assertThat(response.courseStatus()).isNull();
        assertThat(response.courseStaffRole()).isNull();
        verifyNoInteractions(courseStaffRepository);
    }
}
