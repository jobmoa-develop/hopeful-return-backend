package com.jobmoa.hopefulreturn.courseparticipant.entity;

/**
 * 상담사/일정 변경의 변경 주체.
 * FE 변경주체 selectbox 의 (빈칸=NONE / 상담사=COUNSELOR / 참여자=PARTICIPANT) 에 대응한다.
 */
public enum ChangeSubject {
    NONE,
    COUNSELOR,
    PARTICIPANT
}
