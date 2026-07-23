package com.jobmoa.hopefulreturn.courseparticipant.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselingType;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.StaffRole;
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
class ParticipantScopeResolverTest {

    private static final Long USER_ID = 7L;

    @Mock
    private CourseParticipantCounselorRepository courseParticipantCounselorRepository;
    @Mock
    private CourseStaffRepository courseStaffRepository;
    @Mock
    private CourseParticipantRepository courseParticipantRepository;

    @InjectMocks
    private ParticipantScopeResolver resolver;

    private Authentication auth(String... roleAuthorities) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roleAuthorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken("user", null, authorities);
    }

    private CourseStaffEntity staff(Long courseId) {
        return CourseStaffEntity.builder()
                .courseId(courseId)
                .userId(USER_ID)
                .staffRole(StaffRole.STAFF)
                .build();
    }

    private CourseParticipantCounselorEntity counselorRow(Long courseParticipantId) {
        return CourseParticipantCounselorEntity.builder()
                .courseParticipantId(courseParticipantId)
                .counselorId(USER_ID)
                .status(CounselingType.PRE_SESSION)
                .build();
    }

    private CourseParticipantEntity cp(Long courseParticipantId, Long participantId, Long courseId) {
        return CourseParticipantEntity.builder()
                .courseParticipantId(courseParticipantId)
                .participantId(participantId)
                .courseId(courseId)
                .build();
    }

    @Test
    @DisplayName("행정인력(OPERATOR)은 관리자급 — 제한 없음(UNRESTRICTED)")
    void operator_unrestricted() {
        ParticipantScope scope = resolver.resolve(auth("ROLE_OPERATOR"), USER_ID);

        assertThat(scope.unrestricted()).isTrue();
        assertThat(scope.courseParticipantIds()).isNull();
        assertThat(scope.participantIds()).isNull();
    }

    @Test
    @DisplayName("관리자(ADMIN)는 제한 없음(UNRESTRICTED)")
    void admin_unrestricted() {
        ParticipantScope scope = resolver.resolve(auth("ROLE_ADMIN"), USER_ID);

        assertThat(scope.unrestricted()).isTrue();
    }

    @Test
    @DisplayName("userId 가 없으면 제한 없음으로 처리한다")
    void nullUserId_unrestricted() {
        ParticipantScope scope = resolver.resolve(auth("ROLE_STAFF"), null);

        assertThat(scope.unrestricted()).isTrue();
    }

    @Test
    @DisplayName("상담사(COUNSELOR) 단독 — 개별 배정된 상담 건만 스코프에 포함한다(현행 유지)")
    void counselorOnly_individualAssignments() {
        when(courseParticipantCounselorRepository.findByCounselorId(USER_ID))
                .thenReturn(List.of(counselorRow(201L), counselorRow(202L)));
        when(courseParticipantRepository.findAllById(any()))
                .thenReturn(List.of(cp(201L, 30L, 15L), cp(202L, 31L, 15L)));

        ParticipantScope scope = resolver.resolve(auth("ROLE_COUNSELOR"), USER_ID);

        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.courseParticipantIds()).containsExactlyInAnyOrder(201L, 202L);
        assertThat(scope.participantIds()).containsExactlyInAnyOrder(30L, 31L);
    }

    @Test
    @DisplayName("진행자(STAFF) 단독 — 배정 회차(course_staff)의 전체 참여자를 스코프에 포함한다(신규)")
    void staffOnly_assignedCourseParticipants() {
        when(courseStaffRepository.findByUserId(USER_ID))
                .thenReturn(List.of(staff(15L), staff(16L)));
        when(courseParticipantRepository.findByCourseIdIn(any()))
                .thenReturn(List.of(cp(101L, 25L, 15L), cp(102L, 26L, 16L)));

        ParticipantScope scope = resolver.resolve(auth("ROLE_STAFF"), USER_ID);

        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.courseParticipantIds()).containsExactlyInAnyOrder(101L, 102L);
        assertThat(scope.participantIds()).containsExactlyInAnyOrder(25L, 26L);
    }

    @Test
    @DisplayName("상담사+진행자를 함께 가지면 두 스코프의 합집합을 반환한다")
    void counselorAndStaff_union() {
        when(courseStaffRepository.findByUserId(USER_ID)).thenReturn(List.of(staff(15L)));
        when(courseParticipantRepository.findByCourseIdIn(any()))
                .thenReturn(List.of(cp(101L, 25L, 15L)));
        when(courseParticipantCounselorRepository.findByCounselorId(USER_ID))
                .thenReturn(List.of(counselorRow(201L)));
        when(courseParticipantRepository.findAllById(any()))
                .thenReturn(List.of(cp(201L, 30L, 99L)));

        ParticipantScope scope = resolver.resolve(auth("ROLE_STAFF", "ROLE_COUNSELOR"), USER_ID);

        assertThat(scope.courseParticipantIds()).containsExactlyInAnyOrder(101L, 201L);
        assertThat(scope.participantIds()).containsExactlyInAnyOrder(25L, 30L);
    }

    @Test
    @DisplayName("배정이 전혀 없는 제한 사용자는 빈 스코프 — 아무것도 조회할 수 없다")
    void restrictedNoAssignment_emptyScope() {
        when(courseStaffRepository.findByUserId(USER_ID)).thenReturn(List.of());

        ParticipantScope scope = resolver.resolve(auth("ROLE_STAFF"), USER_ID);

        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.courseParticipantIds()).isEmpty();
        assertThat(scope.participantIds()).isEmpty();
    }
}
