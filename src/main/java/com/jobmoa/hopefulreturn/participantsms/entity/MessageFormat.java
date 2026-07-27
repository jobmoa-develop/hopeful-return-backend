package com.jobmoa.hopefulreturn.participantsms.entity;

/**
 * 문자 발송 형식. SMS(≤90B) / LMS(≤2000B) / MMS(이미지 포함).
 */
public enum MessageFormat {
    SMS,
    LMS,
    MMS
}
