package com.jobmoa.hopefulreturn.sms;

/**
 * 발송결과 조회로 판정한 수신 건별 전달 상태(공급자 비의존).
 * SUCCESS/FAIL 은 종료 상태, PENDING 은 아직 처리 중(READY/PROCESSING)으로 재조회 대상.
 */
public enum SmsDeliveryState {
    SUCCESS,
    FAIL,
    PENDING
}
