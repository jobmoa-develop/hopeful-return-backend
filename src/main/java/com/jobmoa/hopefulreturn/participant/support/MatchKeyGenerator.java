package com.jobmoa.hopefulreturn.participant.support;

/**
 * 참여자 중복 판별용 {@code matchKey} 생성기.
 *
 * <p>형식: {@code {이니셜}_{생년}_{전화번호 뒤 4자리}}
 * (예: 김철수 / 1978 / 010-5678-1234 → {@code KCS_1978_1234})
 *
 * <ul>
 *   <li><b>이니셜</b>: 이름 각 글자의 로마자 첫 글자(대문자). 한글 음절은 초성(초성이 'ㅇ'이면
 *       중성)을 로마자화한 첫 글자를 사용한다. 로마자 매핑은 매큔-라이샤워 계열 근사표(김→K,
 *       철→C, 수→S)이며 <b>결정적(deterministic) 키 생성</b>이 목적이다. 표기 규칙을 바꾸려면
 *       {@link #LEAD}/{@link #VOWEL} 표만 교체하면 된다.</li>
 *   <li><b>생년</b>: {@code birthYear} (null이면 빈 문자열).</li>
 *   <li><b>뒤 4자리</b>: 전화번호에서 숫자만 추출한 뒤 마지막 4자리.</li>
 * </ul>
 */
public final class MatchKeyGenerator {

    private static final int HANGUL_BASE = 0xAC00;
    private static final int HANGUL_END = 0xD7A3;
    private static final int JUNG_COUNT = 21;
    private static final int JONG_COUNT = 28;
    private static final int PHONE_TAIL_LENGTH = 4;

    /** 초성 19개 → 로마자 첫 글자. index 11('ㅇ')은 중성으로 대체하므로 사용하지 않는다. */
    private static final char[] LEAD = {
        'K', 'K', 'N', 'T', 'T', 'R', 'M', 'P', 'P', 'S', 'S', ' ', 'C', 'C', 'C', 'K', 'T', 'P', 'H'
    };
    /** 중성 21개 → 로마자 첫 글자. 초성이 'ㅇ'인 음절에 사용. */
    private static final char[] VOWEL = {
        'A', 'A', 'Y', 'Y', 'E', 'E', 'Y', 'Y', 'O', 'W', 'W', 'O', 'Y', 'U', 'W', 'W', 'W', 'Y', 'E', 'E', 'I'
    };

    private MatchKeyGenerator() {
    }

    public static String generate(String name, Integer birthYear, String phone) {
        String initials = initials(name);
        String year = birthYear == null ? "" : String.valueOf(birthYear);
        String tail = phoneTail(phone);
        return initials + "_" + year + "_" + tail;
    }

    private static String initials(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= HANGUL_BASE && c <= HANGUL_END) {
                int index = c - HANGUL_BASE;
                int lead = index / (JUNG_COUNT * JONG_COUNT);
                if (lead == 11) { // 'ㅇ' 초성 → 중성의 로마자 첫 글자 사용
                    int jung = (index % (JUNG_COUNT * JONG_COUNT)) / JONG_COUNT;
                    sb.append(VOWEL[jung]);
                } else {
                    sb.append(LEAD[lead]);
                }
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toUpperCase(c));
            }
            // 공백·기호 등은 건너뜀
        }
        return sb.toString();
    }

    private static String phoneTail(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() <= PHONE_TAIL_LENGTH
                ? digits
                : digits.substring(digits.length() - PHONE_TAIL_LENGTH);
    }
}
