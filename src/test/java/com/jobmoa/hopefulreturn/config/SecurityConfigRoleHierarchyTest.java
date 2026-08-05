package com.jobmoa.hopefulreturn.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * ADMIN 슈퍼유저용 역할 계층({@link SecurityConfig#roleHierarchy()}) 단위 검증.
 * ROLE_ADMIN 이 나머지 8개 역할을 함축하는지, 하위 역할은 상위를 얻지 않는지 확인한다.
 * (Spring 이 이 빈을 @PreAuthorize 평가에 자동 연결하는 것은 프레임워크 보장 동작.)
 */
class SecurityConfigRoleHierarchyTest {

    private final RoleHierarchy hierarchy = SecurityConfig.roleHierarchy();

    private static final List<String> ALL_ROLE_AUTHORITIES = List.of(
            "ROLE_ADMIN", "ROLE_HEAD_OFFICE", "ROLE_REGIONAL_MANAGER", "ROLE_OPERATOR",
            "ROLE_COUNSELOR", "ROLE_STAFF", "ROLE_LECTURER", "ROLE_PROJECT_MANAGER",
            "ROLE_PROJECT_LEADER");

    @Test
    @DisplayName("ROLE_ADMIN 은 나머지 모든 역할 권한을 함축한다")
    void adminImpliesAllOtherRoles() {
        List<String> reachable = reachable("ROLE_ADMIN");

        assertThat(reachable).containsAll(ALL_ROLE_AUTHORITIES);
    }

    @Test
    @DisplayName("ADMIN 은 현재 ADMIN 이 빠져있는 출결(OPERATOR/STAFF)·조퇴외출(OPERATOR) 권한도 얻는다")
    void adminReachesAttendanceOnlyRoles() {
        List<String> reachable = reachable("ROLE_ADMIN");

        assertThat(reachable).contains("ROLE_OPERATOR", "ROLE_STAFF");
    }

    @Test
    @DisplayName("하위 역할(COUNSELOR)은 상위(ADMIN) 권한을 얻지 못한다 — 데이터 스코프 회귀 방지")
    void nonAdminDoesNotReachAdmin() {
        List<String> reachable = reachable("ROLE_COUNSELOR");

        assertThat(reachable).containsExactly("ROLE_COUNSELOR");
    }

    private List<String> reachable(String roleAuthority) {
        return hierarchy
                .getReachableGrantedAuthorities(List.of(new SimpleGrantedAuthority(roleAuthority)))
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
