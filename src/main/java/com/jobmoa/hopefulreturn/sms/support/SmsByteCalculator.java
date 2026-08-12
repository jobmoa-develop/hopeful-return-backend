package com.jobmoa.hopefulreturn.sms.support;

import java.nio.charset.Charset;

/**
 * 문자 본문/제목의 바이트 길이 계산과 SMS/LMS 판별을 담당하는 공용 유틸.
 * 참여자 SMS(ParticipantSmsServiceImpl)와 강좌 담당자 SMS(CourseStaffSmsServiceImpl)가 공유한다.
 *
 * <p>바이트 계산은 통신사 SMS 규격과 동일하게 EUC-KR 기준(ASCII 1바이트, 한글 등 2바이트)이다.
 */
public final class SmsByteCalculator {

    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    /** SMS 최대 바이트(초과 시 LMS). */
    public static final int SMS_MAX_BYTES = 90;
    /** LMS 최대 바이트(초과 시 발송 불가). */
    public static final int LMS_MAX_BYTES = 2000;
    /** LMS/MMS 제목 최대 바이트. */
    public static final int SUBJECT_MAX_BYTES = 40;

    private SmsByteCalculator() {
    }

    /** EUC-KR 기준 바이트 길이. null 은 0. */
    public static int byteLength(String value) {
        return value == null ? 0 : value.getBytes(EUC_KR).length;
    }

    /** 이미지 없는 텍스트 문자에서 최대 바이트로 SMS(≤90)/LMS 판별. 반환값은 SmsSendCommand.type 문자열. */
    public static String resolveTextFormat(int maxContentBytes) {
        return maxContentBytes <= SMS_MAX_BYTES ? "SMS" : "LMS";
    }
}
