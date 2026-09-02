package com.jobmoa.hopefulreturn.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseRequest;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseStatusRequest;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.course.scope.CourseScope;
import com.jobmoa.hopefulreturn.coursedailystaff.exception.AssignConflictException;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.AssignConflict;
import com.jobmoa.hopefulreturn.coursedailystaff.service.CourseDailyStaffService;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import java.time.LocalDate;
import java.util.Map;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private RegionRepository regionRepository;
    @Mock
    private CourseParticipantRepository courseParticipantRepository;
    @Mock
    private CourseStaffRepository courseStaffRepository;
    @Mock
    private CourseDailyStaffService courseDailyStaffService;

    @InjectMocks
    private CourseServiceImpl service;

    @Test
    @DisplayName("course list specification includes allowed course IDs")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findAll_scoped_addsCourseIdPredicate() {
        when(courseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Specification<CourseEntity>> captor = ArgumentCaptor.forClass(Specification.class);

        service.findAll(null, null, null, null, null, null, new CourseScope(Set.of(100L, 200L)), null, null, 0, 10);
        verify(courseRepository).findAll(captor.capture(), any(Pageable.class));

        Root<CourseEntity> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Path<Object> courseIdPath = org.mockito.Mockito.mock(Path.class);
        Predicate inPredicate = org.mockito.Mockito.mock(Predicate.class);
        Predicate andPredicate = org.mockito.Mockito.mock(Predicate.class);
        when(root.get("courseId")).thenReturn(courseIdPath);
        when(courseIdPath.in(Set.of(100L, 200L))).thenReturn(inPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);
        // count 쿼리(Long)로 취급해 상태 우선순위 orderBy 부여 블록을 건너뛴다(이 테스트는 예측자만 검증).
        org.mockito.Mockito.doReturn(Long.class).when(query).getResultType();

        Predicate result = captor.getValue().toPredicate(root, query, cb);

        assertThat(result).isSameAs(andPredicate);
        verify(courseIdPath).in(Set.of(100L, 200L));
    }

    @Test
    @DisplayName("empty scoped course list uses disjunction for zero rows")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findAll_emptyScope_disjunction() {
        when(courseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Specification<CourseEntity>> captor = ArgumentCaptor.forClass(Specification.class);

        service.findAll(null, null, null, null, null, null, new CourseScope(Set.of()), null, null, 0, 10);
        verify(courseRepository).findAll(captor.capture(), any(Pageable.class));

        Root<CourseEntity> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Predicate falsePredicate = org.mockito.Mockito.mock(Predicate.class);
        Predicate andPredicate = org.mockito.Mockito.mock(Predicate.class);
        when(cb.disjunction()).thenReturn(falsePredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);
        org.mockito.Mockito.doReturn(Long.class).when(query).getResultType();

        Predicate result = captor.getValue().toPredicate(root, query, cb);

        assertThat(result).isSameAs(andPredicate);
        verify(cb).disjunction();
    }

    @Test
    @DisplayName("회차번호(courseNumber) 검색 시 courseNumber equal 예측자를 추가한다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findAll_courseNumber_addsEqualPredicate() {
        when(courseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Specification<CourseEntity>> captor = ArgumentCaptor.forClass(Specification.class);

        service.findAll(null, null, null, null, 20, null, CourseScope.UNRESTRICTED, null, null, 0, 10);
        verify(courseRepository).findAll(captor.capture(), any(Pageable.class));

        Root<CourseEntity> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Path<Object> courseNumberPath = org.mockito.Mockito.mock(Path.class);
        Predicate eqPredicate = org.mockito.Mockito.mock(Predicate.class);
        Predicate andPredicate = org.mockito.Mockito.mock(Predicate.class);
        when(root.get("courseNumber")).thenReturn(courseNumberPath);
        when(cb.equal(courseNumberPath, 20)).thenReturn(eqPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);
        org.mockito.Mockito.doReturn(Long.class).when(query).getResultType();

        Predicate result = captor.getValue().toPredicate(root, query, cb);

        assertThat(result).isSameAs(andPredicate);
        verify(cb).equal(courseNumberPath, 20);
    }

    @Test
    @DisplayName("지역회차(localCourseNumber) 검색 시 localCourseNumber equal 예측자를 추가한다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findAll_localCourseNumber_addsEqualPredicate() {
        when(courseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Specification<CourseEntity>> captor = ArgumentCaptor.forClass(Specification.class);

        service.findAll(null, null, null, null, null, 3, CourseScope.UNRESTRICTED, null, null, 0, 10);
        verify(courseRepository).findAll(captor.capture(), any(Pageable.class));

        Root<CourseEntity> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Path<Object> localPath = org.mockito.Mockito.mock(Path.class);
        Predicate eqPredicate = org.mockito.Mockito.mock(Predicate.class);
        Predicate andPredicate = org.mockito.Mockito.mock(Predicate.class);
        when(root.get("localCourseNumber")).thenReturn(localPath);
        when(cb.equal(localPath, 3)).thenReturn(eqPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);
        org.mockito.Mockito.doReturn(Long.class).when(query).getResultType();

        Predicate result = captor.getValue().toPredicate(root, query, cb);

        assertThat(result).isSameAs(andPredicate);
        verify(cb).equal(localPath, 3);
    }

    @Test
    @DisplayName("정렬 미지정 시 Pageable 정렬은 비우고(spec orderBy 유지) Specification 이 상태 우선순위 CASE orderBy 를 심는다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findAll_defaultSort_leavesPageableUnsortedAndSpecOrders() {
        when(courseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Specification<CourseEntity>> specCaptor = ArgumentCaptor.forClass(Specification.class);

        service.findAll(null, null, null, null, null, null, CourseScope.UNRESTRICTED, null, null, 0, 10);
        verify(courseRepository).findAll(specCaptor.capture(), pageableCaptor.capture());

        // 기본 정렬은 Pageable 을 비운다 → Spring Data 가 spec 의 orderBy 를 그대로 유지한다.
        assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();

        // 데이터 쿼리(비 count)면 spec 이 상태 우선순위 CASE 로 query.orderBy 를 호출한다.
        Root<CourseEntity> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        CriteriaBuilder.Case caseExpr = org.mockito.Mockito.mock(
                CriteriaBuilder.Case.class, org.mockito.Mockito.RETURNS_SELF);
        org.mockito.Mockito.doReturn(CourseEntity.class).when(query).getResultType();
        when(cb.selectCase()).thenReturn(caseExpr);

        specCaptor.getValue().toPredicate(root, query, cb);

        verify(cb).selectCase(); // 상태 우선순위 orderBy 분기가 실행됨
        verify(query).orderBy(org.mockito.ArgumentMatchers.<jakarta.persistence.criteria.Order>any(),
                org.mockito.ArgumentMatchers.<jakarta.persistence.criteria.Order>any());
    }

    @Test
    @DisplayName("정렬 지정 시 해당 컬럼 + courseId tiebreaker 로 정렬한다(CASE 없이 Pageable Sort)")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findAll_explicitSort_honorsColumnAndTiebreaker() {
        when(courseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        service.findAll(null, null, null, null, null, null, CourseScope.UNRESTRICTED, "courseName", "desc", 0, 10);
        verify(courseRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Sort sort = pageableCaptor.getValue().getSort();
        assertThat(sort.getOrderFor("courseName")).isNotNull();
        assertThat(sort.getOrderFor("courseName").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("courseId")).isNotNull();
        assertThat(sort.getOrderFor("courseId").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("화이트리스트에 없는 정렬 키는 기본(상태 우선순위) 정렬로 폴백 — Pageable 정렬을 비운다")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findAll_unknownSort_leavesPageableUnsorted() {
        when(courseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        service.findAll(null, null, null, null, null, null, CourseScope.UNRESTRICTED, "bogus", "asc", 0, 10);
        verify(courseRepository).findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    @DisplayName("unassigned course participants request is denied before participant query")
    void findParticipants_unassignedCourse_forbidden() {
        when(courseRepository.findById(300L)).thenReturn(Optional.of(course(300L)));

        assertThatThrownBy(() -> service.findParticipants(
                300L, null, null, new CourseScope(Set.of(100L, 200L)), 0, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(courseParticipantRepository, never()).findPageByCourseIdAndFilters(any(), any(), any(), any());
    }

    @Test
    @DisplayName("assigned course participants use DB pageable query and preserve totals")
    void findParticipants_assignedCourse_pageableQuery() {
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course(100L)));
        CourseParticipantEntity first = cp(1L, "Kim", CourseParticipantStatus.APPLIED);
        CourseParticipantEntity second = cp(2L, "Kimura", CourseParticipantStatus.CONFIRMED);
        when(courseParticipantRepository.findPageByCourseIdAndFilters(
                eq(100L), eq(CourseParticipantStatus.APPLIED), eq("Kim"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), Pageable.ofSize(2), 5));

        CourseParticipantListResponse response = service.findParticipants(
                100L, "APPLIED", " Kim ", new CourseScope(Set.of(100L)), 0, 2);

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("OPEN 상태 변경 시 교육 시작시간이 없으면 COURSE_EDUCATION_START_TIME_NOT_SET 예외")
    void updateStatus_openWithoutEducationStartTime_rejected() {
        when(courseRepository.findById(300L))
                .thenReturn(Optional.of(course(300L, null, LocalTime.of(18, 0))));

        assertThatThrownBy(() -> service.updateStatus(300L, new UpdateCourseStatusRequest("OPEN"), 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.COURSE_EDUCATION_START_TIME_NOT_SET);
                    assertThat(be.getMessage()).contains("courseId=300", "교육 시작 시간");
                });
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("IN_PROGRESS 상태 변경 시 교육 종료시간이 없으면 COURSE_EDUCATION_END_TIME_NOT_SET 예외")
    void updateStatus_inProgressWithoutEducationEndTime_rejected() {
        when(courseRepository.findById(300L))
                .thenReturn(Optional.of(course(300L, LocalTime.of(9, 0), null)));

        assertThatThrownBy(() -> service.updateStatus(300L, new UpdateCourseStatusRequest("IN_PROGRESS"), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_EDUCATION_END_TIME_NOT_SET);
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("교육시간이 있으면 실사용 상태 변경을 허용한다")
    void updateStatus_activeWithEducationTimes_success() {
        CourseEntity course = course(300L, LocalTime.of(9, 0), LocalTime.of(18, 0));
        when(courseRepository.findById(300L)).thenReturn(Optional.of(course));

        service.updateStatus(300L, new UpdateCourseStatusRequest("RECRUITING"), 1L);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.RECRUITING);
        verify(courseRepository).save(course);
    }

    @Test
    @DisplayName("교육일 이동 수정 시 배정 인력 일정 재동기화를 위임한다(moved 산출)")
    void update_movedDay_delegatesRemap() {
        CourseEntity existing = CourseEntity.builder()
                .courseId(300L).regionId(1L).courseNumber(1).localCourseNumber(1).courseName("c")
                .capacity(10).minimumCapacity(1)
                .day1Date(LocalDate.of(2026, 8, 17))
                .day2Date(LocalDate.of(2026, 8, 18))
                .build();
        when(courseRepository.findById(300L)).thenReturn(Optional.of(existing));

        // day2 만 8/18 → 8/20 으로 변경(나머지 null = 미변경). 충돌 없음(detect 기본 빈 목록).
        UpdateCourseRequest req = new UpdateCourseRequest(
                null, null, null, null, null, null,
                null, LocalDate.of(2026, 8, 20), null, null, null,
                null, null, null, null, null, null, null, null);
        service.update(300L, req);

        verify(courseRepository).save(existing);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<LocalDate, LocalDate>> movedCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<LocalDate>> removedCaptor = ArgumentCaptor.forClass(Set.class);
        verify(courseDailyStaffService)
                .remapAssignmentDates(eq(300L), movedCaptor.capture(), removedCaptor.capture());
        assertThat(movedCaptor.getValue())
                .hasSize(1)
                .containsEntry(LocalDate.of(2026, 8, 18), LocalDate.of(2026, 8, 20));
        assertThat(removedCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("교육일 미변경 수정이면 인력 일정 재동기화를 호출하지 않는다")
    void update_noDayChange_skipsRemap() {
        CourseEntity existing = CourseEntity.builder()
                .courseId(300L).regionId(1L).courseNumber(1).localCourseNumber(1).courseName("c")
                .capacity(10).minimumCapacity(1)
                .day1Date(LocalDate.of(2026, 8, 17))
                .build();
        when(courseRepository.findById(300L)).thenReturn(Optional.of(existing));

        // 교육일 필드는 모두 null(미변경), 강좌명만 변경.
        UpdateCourseRequest req = new UpdateCourseRequest(
                null, null, null, "새이름", null, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        service.update(300L, req);

        verify(courseRepository).save(existing);
        verify(courseDailyStaffService, never()).detectDateChangeConflicts(any(), any());
        verify(courseDailyStaffService, never()).remapAssignmentDates(any(), any(), any());
    }

    @Test
    @DisplayName("교육일 이동에 충돌이 있고 미확인이면 AssignConflictException — 회차 저장·재동기화 안 함")
    void update_conflictUnconfirmed_throwsAndDoesNotSave() {
        CourseEntity existing = CourseEntity.builder()
                .courseId(300L).regionId(1L).courseNumber(1).localCourseNumber(1).courseName("c")
                .capacity(10).minimumCapacity(1)
                .day1Date(LocalDate.of(2026, 8, 17))
                .day2Date(LocalDate.of(2026, 8, 18))
                .build();
        when(courseRepository.findById(300L)).thenReturn(Optional.of(existing));
        when(courseDailyStaffService.detectDateChangeConflicts(eq(300L), any()))
                .thenReturn(List.of(new AssignConflict(
                        6L, "이강사", LocalDate.of(2026, 8, 20), "AM", 99L, "타회차", "LECTURER")));

        // day2 8/18 → 8/20, confirmConflicts 미지정(null)
        UpdateCourseRequest req = new UpdateCourseRequest(
                null, null, null, null, null, null,
                null, LocalDate.of(2026, 8, 20), null, null, null,
                null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.update(300L, req))
                .isInstanceOf(AssignConflictException.class);
        verify(courseRepository, never()).save(any());
        verify(courseDailyStaffService, never()).remapAssignmentDates(any(), any(), any());
    }

    @Test
    @DisplayName("충돌이 있어도 confirmConflicts=true면 저장하고 재동기화를 진행한다")
    void update_conflictConfirmed_proceeds() {
        CourseEntity existing = CourseEntity.builder()
                .courseId(300L).regionId(1L).courseNumber(1).localCourseNumber(1).courseName("c")
                .capacity(10).minimumCapacity(1)
                .day1Date(LocalDate.of(2026, 8, 17))
                .day2Date(LocalDate.of(2026, 8, 18))
                .build();
        when(courseRepository.findById(300L)).thenReturn(Optional.of(existing));
        when(courseDailyStaffService.detectDateChangeConflicts(eq(300L), any()))
                .thenReturn(List.of(new AssignConflict(
                        6L, "이강사", LocalDate.of(2026, 8, 20), "AM", 99L, "타회차", "LECTURER")));

        // day2 8/18 → 8/20, confirmConflicts=true
        UpdateCourseRequest req = new UpdateCourseRequest(
                null, null, null, null, null, null,
                null, LocalDate.of(2026, 8, 20), null, null, null,
                null, null, null, null, null, null, null, true);
        service.update(300L, req);

        verify(courseRepository).save(existing);
        verify(courseDailyStaffService).remapAssignmentDates(eq(300L), any(), any());
    }

    @Test
    @DisplayName("회차 폐강(CANCELED) 전환 시 수료·미수료 제외 참여자를 폐강(COURSE_CANCELED)으로 바꾸고 이전 상태를 저장한다")
    void updateStatus_toCanceled_propagatesParticipants() {
        CourseEntity course = course(300L, LocalTime.of(9, 0), LocalTime.of(18, 0));
        course.setStatus(CourseStatus.IN_PROGRESS);
        when(courseRepository.findById(300L)).thenReturn(Optional.of(course));
        CourseParticipantEntity applied = cp(1L, "가", CourseParticipantStatus.APPLIED);
        CourseParticipantEntity confirmed = cp(2L, "나", CourseParticipantStatus.CONFIRMED);
        CourseParticipantEntity canceled = cp(3L, "다", CourseParticipantStatus.CANCELED);
        CourseParticipantEntity completed = cp(4L, "라", CourseParticipantStatus.COMPLETED);
        CourseParticipantEntity incomplete = cp(5L, "마", CourseParticipantStatus.INCOMPLETE);
        when(courseParticipantRepository.findByCourseId(300L))
                .thenReturn(List.of(applied, confirmed, canceled, completed, incomplete));

        service.updateStatus(300L, new UpdateCourseStatusRequest("CANCELED"), 1L);

        assertThat(applied.getStatus()).isEqualTo(CourseParticipantStatus.COURSE_CANCELED);
        assertThat(applied.getPreCancelStatus()).isEqualTo("APPLIED");
        assertThat(confirmed.getStatus()).isEqualTo(CourseParticipantStatus.COURSE_CANCELED);
        assertThat(confirmed.getPreCancelStatus()).isEqualTo("CONFIRMED");
        assertThat(canceled.getStatus()).isEqualTo(CourseParticipantStatus.COURSE_CANCELED);
        assertThat(canceled.getPreCancelStatus()).isEqualTo("CANCELED");
        // 수료·미수료는 이력 보존 — 변경하지 않는다.
        assertThat(completed.getStatus()).isEqualTo(CourseParticipantStatus.COMPLETED);
        assertThat(completed.getPreCancelStatus()).isNull();
        assertThat(incomplete.getStatus()).isEqualTo(CourseParticipantStatus.INCOMPLETE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CourseParticipantEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(courseParticipantRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(applied, confirmed, canceled);
    }

    @Test
    @DisplayName("폐강 회차를 다시 활성 상태로 되돌리면 폐강 참여자만 이전 상태로 복구한다(없으면 CONFIRMED 폴백)")
    void updateStatus_revertFromCanceled_restoresParticipants() {
        CourseEntity course = course(300L, LocalTime.of(9, 0), LocalTime.of(18, 0));
        course.setStatus(CourseStatus.CANCELED);
        when(courseRepository.findById(300L)).thenReturn(Optional.of(course));
        CourseParticipantEntity restored = cp(1L, "가", CourseParticipantStatus.COURSE_CANCELED);
        restored.setPreCancelStatus("APPLIED");
        CourseParticipantEntity noPre = cp(2L, "나", CourseParticipantStatus.COURSE_CANCELED);
        noPre.setPreCancelStatus(null);
        CourseParticipantEntity completed = cp(3L, "다", CourseParticipantStatus.COMPLETED);
        when(courseParticipantRepository.findByCourseId(300L))
                .thenReturn(List.of(restored, noPre, completed));

        service.updateStatus(300L, new UpdateCourseStatusRequest("RECRUITING"), 1L);

        assertThat(restored.getStatus()).isEqualTo(CourseParticipantStatus.APPLIED);
        assertThat(restored.getPreCancelStatus()).isNull();
        // 저장된 이전 상태가 없으면 선정(CONFIRMED)으로 폴백.
        assertThat(noPre.getStatus()).isEqualTo(CourseParticipantStatus.CONFIRMED);
        // 폐강 상태가 아니던 참여자는 건드리지 않는다.
        assertThat(completed.getStatus()).isEqualTo(CourseParticipantStatus.COMPLETED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CourseParticipantEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(courseParticipantRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(restored, noPre);
    }

    private CourseEntity course(Long courseId) {
        return course(courseId, null, null);
    }

    private CourseEntity course(Long courseId, LocalTime educationStartTime, LocalTime educationEndTime) {
        return CourseEntity.builder()
                .courseId(courseId)
                .regionId(1L)
                .courseNumber(1)
                .localCourseNumber(1)
                .courseName("course")
                .capacity(10)
                .minimumCapacity(1)
                .educationStartTime(educationStartTime)
                .educationEndTime(educationEndTime)
                .build();
    }

    private CourseParticipantEntity cp(Long id, String participantName, CourseParticipantStatus status) {
        return CourseParticipantEntity.builder()
                .courseParticipantId(id)
                .courseId(100L)
                .participantId(id + 10)
                .participant(ParticipantEntity.builder().name(participantName).phone("010").build())
                .status(status)
                .build();
    }
}