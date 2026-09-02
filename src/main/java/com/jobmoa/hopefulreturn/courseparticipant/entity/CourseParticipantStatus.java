package com.jobmoa.hopefulreturn.courseparticipant.entity;

public enum CourseParticipantStatus {
    APPLIED,         // 접수
    CONFIRMED,       // 선정
    CANCELED,        // 취소 (개별 수강 취소)
    COMPLETED,       // 수료
    INCOMPLETE,      // 미수료
    COURSE_CANCELED  // 폐강 (회차 폐강 시 시스템이 자동 세팅)
}
