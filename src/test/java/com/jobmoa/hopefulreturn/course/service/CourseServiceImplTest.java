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
import com.jobmoa.hopefulreturn.course.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.course.scope.CourseScope;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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

    @InjectMocks
    private CourseServiceImpl service;

    @Test
    @DisplayName("course list specification includes allowed course IDs")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findAll_scoped_addsCourseIdPredicate() {
        when(courseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Specification<CourseEntity>> captor = ArgumentCaptor.forClass(Specification.class);

        service.findAll(null, null, null, null, new CourseScope(Set.of(100L, 200L)), 0, 10);
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

        service.findAll(null, null, null, null, new CourseScope(Set.of()), 0, 10);
        verify(courseRepository).findAll(captor.capture(), any(Pageable.class));

        Root<CourseEntity> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Predicate falsePredicate = org.mockito.Mockito.mock(Predicate.class);
        Predicate andPredicate = org.mockito.Mockito.mock(Predicate.class);
        when(cb.disjunction()).thenReturn(falsePredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

        Predicate result = captor.getValue().toPredicate(root, query, cb);

        assertThat(result).isSameAs(andPredicate);
        verify(cb).disjunction();
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

    private CourseEntity course(Long courseId) {
        return CourseEntity.builder()
                .courseId(courseId)
                .regionId(1L)
                .courseNumber(1)
                .localCourseNumber(1)
                .courseName("course")
                .capacity(10)
                .minimumCapacity(1)
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
