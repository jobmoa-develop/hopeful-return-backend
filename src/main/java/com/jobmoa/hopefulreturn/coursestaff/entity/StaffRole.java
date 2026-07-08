package com.jobmoa.hopefulreturn.coursestaff.entity;

/**
 * 강좌 스태프 배정 역할 — course_staff.staff_role(NVARCHAR)에 문자열로 저장된다.
 * PM/PL도 강좌 스태프로 배정될 수 있어 RoleName과 동일하게 확장한다.
 */
public enum StaffRole {
    COUNSELOR,         // 상담사
    LECTURER,          // 강사
    STAFF,             // 진행요원(진행자)
    PROJECT_MANAGER,   // 프로젝트 매니저(PM)
    PROJECT_LEADER     // 프로젝트 리더(PL)
}
