package com.jobmoa.hopefulreturn.coursestaffsms.entity;

public enum StaffNotifyType {
    STATUS_CHANGE,
    SCHEDULE_CHANGE,
    ASSIGN_NEW,       // 최초 배정
    ASSIGN_CHANGED,   // 배정 수정 - 추가/변동 인원
    ASSIGN_REMOVED    // 배정 수정 - 제외 인원
}