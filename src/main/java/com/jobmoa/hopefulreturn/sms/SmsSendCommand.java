package com.jobmoa.hopefulreturn.sms;

import java.util.List;

/**
 * 문자 발송 명령(공급자 비의존).
 * recipients 의 content 는 {name}/{region}/{round} 치환이 끝난 최종 본문이며, 호출자가 100건 이하로 분할해 전달한다.
 * imagesBase64 는 MMS 첨부(원본 Base64, data URI 접두어 포함 가능 — 구현에서 제거).
 * reserveTime 이 있으면 예약 발송(SENS reserveTime, "yyyy-MM-dd HH:mm"), reserveTimeZone 은 예약 시각의 시간대(예: "Asia/Seoul").
 */
public record SmsSendCommand(
        String type,
        String subject,
        String content,
        List<Recipient> recipients,
        List<String> imagesBase64,
        String reserveTime,
        String reserveTimeZone
) {

    public record Recipient(String to, String content) {
    }
}
