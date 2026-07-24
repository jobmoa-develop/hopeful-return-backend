package com.jobmoa.hopefulreturn.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * 역할 기반 데이터 스코프 판정 공용 유틸.
 * COUNSELOR(상담사) 전용 사용자만 배정 참여자로 스코프를 제한하고, 관리자 롤을 함께
 * 보유한 사용자에게는 제한을 걸지 않는다. 수강생/사후관리 등 여러 도메인에서 재사용한다.
 */
public final class AuthScopeSupport {

    private AuthScopeSupport() {
    }

    /**
     * 권한이 COUNSELOR 만 있는 사용자인지 판정한다. 관리자 롤을 함께 가진 경우 false(스코프 제한 없음).
     */
    public static boolean isCounselorOnly(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        boolean hasCounselor = false;
        boolean hasBroaderRole = false;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_COUNSELOR".equals(role)) {
                hasCounselor = true;
            } else if (role.startsWith("ROLE_")) {
                hasBroaderRole = true;
            }
        }
        return hasCounselor && !hasBroaderRole;
    }

    /**
     * 권한이 STAFF 만 있는 사용자인지 판정한다.
     * 다른 관리자/상위 롤을 함께 가진 경우 false(스코프 제한 없음).
     */
    public static boolean isStaffOnly(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        boolean hasStaff = false;
        boolean hasBroaderRole = false;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();

            if ("ROLE_STAFF".equals(role)) {
                hasStaff = true;
            } else if (role.startsWith("ROLE_")) {
                hasBroaderRole = true;
            }
        }

        return hasStaff && !hasBroaderRole;
    }
}