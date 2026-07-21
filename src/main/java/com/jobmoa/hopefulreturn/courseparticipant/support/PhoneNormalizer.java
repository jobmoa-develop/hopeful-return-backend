package com.jobmoa.hopefulreturn.courseparticipant.support;

/**
 * 휴대폰번호 정규화 — 숫자만 남기고 엑셀 저장 형태의 편차를 흡수한다.
 *
 * <ul>
 *   <li>11자리(정상 `01012345678`) → 그대로</li>
 *   <li>13자(하이픈 `010-1234-5678`) → 숫자만 추출해 11자리</li>
 *   <li>10자리(엑셀이 숫자로 저장하며 선행 0 누락, 예 `1012345678`) → 앞에 `0` 접두 → 11자리</li>
 * </ul>
 */
public final class PhoneNormalizer {

    private static final int LEADING_ZERO_DROPPED_LENGTH = 10;

    private PhoneNormalizer() {
    }

    /** 숫자만 남기고, 선행 0이 빠진 10자리는 0을 붙인다. 값이 없으면 null. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.length() == LEADING_ZERO_DROPPED_LENGTH && !digits.startsWith("0")) {
            digits = "0" + digits;
        }
        return digits;
    }
}
