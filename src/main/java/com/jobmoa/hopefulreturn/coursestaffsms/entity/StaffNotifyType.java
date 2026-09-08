package com.jobmoa.hopefulreturn.coursestaffsms.entity;

public enum StaffNotifyType {
    STATUS_CHANGE,
    SCHEDULE_CHANGE,
    ASSIGN_NEW,       // 최초 배정
    ASSIGN_CHANGED,   // 배정 수정 - 추가/변동 인원
    ASSIGN_REMOVED,   // 배정 수정 - 제외 인원
    COURSE_OPEN_REMINDER // 개강 하루전 자동 안내(스케줄러 발송)
}