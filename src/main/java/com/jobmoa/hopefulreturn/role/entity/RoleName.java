package com.jobmoa.hopefulreturn.role.entity;

/**
 * 시스템 사용자 역할(권한). role 테이블의 role_name과 1:1로 매핑된다.
 */
public enum RoleName {
    HEAD_OFFICE,       // 본부장
    REGIONAL_MANAGER,  // 지역담당자
    OPERATOR,          // 행정허브(행정)
    COUNSELOR,         // 상담사
    LECTURER,          // 강사
    STAFF,             // 진행요원(진행자)
    ADMIN,             // 시스템 관리자
    PROJECT_MANAGER,   // 프로젝트 매니저(PM)
    PROJECT_LEADER     // 프로젝트 리더(PL)
}
