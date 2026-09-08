package com.jobmoa.hopefulreturn.courseparticipant.entity;

/**
 * 상담사 변경 이력의 변경 종류.
 * COUNSELOR_CHANGE — 상담사 배정 변경, SCHEDULE_CHANGE — 상담 시작/완료 일시 변경,
 * UNAVAILABILITY_TOGGLE — 상담 불가 여부 토글 변경.
 */
public enum CounselorChangeType {
    COUNSELOR_CHANGE,
    SCHEDULE_CHANGE,
    UNAVAILABILITY_TOGGLE
}
