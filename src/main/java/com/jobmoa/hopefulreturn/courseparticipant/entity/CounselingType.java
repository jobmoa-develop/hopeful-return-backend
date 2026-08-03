package com.jobmoa.hopefulreturn.courseparticipant.entity;

/**
 * 상담사 배정 슬롯 구분 — 사전상담(PRE_SESSION) / 사후상담1(POST_SESSION_1) / 사후상담2(POST_SESSION_2).
 * 수강건당 슬롯별 상담사 1명 (UQ_CPC_PARTICIPANT_STATUS).
 */
public enum CounselingType {
    PRE_SESSION,
    POST_SESSION_1,
    POST_SESSION_2
}
