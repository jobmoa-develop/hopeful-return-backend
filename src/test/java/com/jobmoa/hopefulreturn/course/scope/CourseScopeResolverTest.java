package com.jobmoa.hopefulreturn.course.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class CourseScopeResolverTest {

    private static final Long USER_ID = 10L;

    @Mock
    private CourseStaffRepository courseStaffRepository;

    @InjectMocks
    private CourseScopeResolver resolver;

    private Authentication auth(String... roleAuthorities) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roleAuthorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken("user", null, authorities);
    }

    private CourseStaffEntity assignment(Long courseId) {
        return CourseStaffEntity.builder()
                .userId(USER_ID)
                .courseId(courseId)
                .build();
    }

    @Test
    @DisplayName("admin roles are unrestricted")
    void admin_unrestricted() {
        CourseScope scope = resolver.resolve(auth("ROLE_ADMIN"), USER_ID);

        assertThat(scope.unrestricted()).isTrue();
        assertThat(scope.allowsCourse(999L)).isTrue();
    }

    @Test
    @DisplayName("admin role wins when combined with a restricted role")
    void adminAndRestricted_unrestricted() {
        CourseScope scope = resolver.resolve(auth("ROLE_ADMIN", "ROLE_OPERATOR"), USER_ID);

        assertThat(scope.unrestricted()).isTrue();
    }

    @Test
    @DisplayName("STAFF is scoped to assigned course IDs")
    void staff_assignedCourses() {
        when(courseStaffRepository.findByUserId(USER_ID))
                .thenReturn(List.of(assignment(100L), assignment(200L)));

        CourseScope scope = resolver.resolve(auth("ROLE_STAFF"), USER_ID);

        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.courseIds()).containsExactlyInAnyOrder(100L, 200L);
        assertThat(scope.allowsCourse(100L)).isTrue();
        assertThat(scope.allowsCourse(300L)).isFalse();
    }

    @Test
    @DisplayName("LECTURER is scoped to assigned course IDs")
    void lecturer_assignedCourses() {
        when(courseStaffRepository.findByUserId(USER_ID)).thenReturn(List.of(assignment(100L)));

        CourseScope scope = resolver.resolve(auth("ROLE_LECTURER"), USER_ID);

        assertThat(scope.courseIds()).containsExactly(100L);
    }

    @Test
    @DisplayName("OPERATOR is scoped to assigned course IDs")
    void operator_assignedCourses() {
        when(courseStaffRepository.findByUserId(USER_ID)).thenReturn(List.of(assignment(100L)));

        CourseScope scope = resolver.resolve(auth("ROLE_OPERATOR"), USER_ID);

        assertThat(scope.courseIds()).containsExactly(100L);
    }

    @Test
    @DisplayName("PROJECT_LEADER is scoped to assigned course IDs")
    void projectLeader_assignedCourses() {
        when(courseStaffRepository.findByUserId(USER_ID)).thenReturn(List.of(assignment(100L)));

        CourseScope scope = resolver.resolve(auth("ROLE_PROJECT_LEADER"), USER_ID);

        assertThat(scope.courseIds()).containsExactly(100L);
    }

    @Test
    @DisplayName("restricted user without assignments has an empty scope")
    void restrictedNoAssignment_emptyScope() {
        when(courseStaffRepository.findByUserId(USER_ID)).thenReturn(List.of());

        CourseScope scope = resolver.resolve(auth("ROLE_STAFF"), USER_ID);

        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.courseIds()).isEmpty();
        assertThat(scope.allowsCourse(100L)).isFalse();
    }
}
