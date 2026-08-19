package com.jobmoa.hopefulreturn.staffunavailablenotice.entity;

/** 근무불가 메일 알림 발송 상태. 폴링·예약 없이 발송 시점의 성공/실패만 기록한다. */
public enum NoticeSendStatus {
    SUCCESS,
    FAIL
}
