package com.jobmoa.hopefulreturn.participantsms.entity;

public enum SendStatus {
    SUCCESS,
    FAIL,
    PENDING,
    // 예약 발송(SENS reserveTime) 접수 후 예약시각 도래 전. 도래 시 PENDING 으로 승격된다.
    RESERVED,
    // 예약이 취소됨(SENS 예약취소). 실제 발송되지 않는다.
    CANCELED
}
