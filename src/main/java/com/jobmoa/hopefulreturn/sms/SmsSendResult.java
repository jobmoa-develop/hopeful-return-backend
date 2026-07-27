package com.jobmoa.hopefulreturn.sms;

import java.util.List;

/**
 * 문자 발송 결과. SENS 는 접수 성공 시 statusCode "202" 를 반환하며 실제 전달은 비동기다.
 * fileIds 는 MMS 첨부 업로드로 받은 SENS 파일 ID(이력 저장용, 없으면 빈 목록).
 */
public record SmsSendResult(
        boolean success, String statusCode, String statusName, String requestId, List<String> fileIds) {

    public static SmsSendResult ok(String statusCode, String statusName, String requestId, List<String> fileIds) {
        return new SmsSendResult(true, statusCode, statusName, requestId, fileIds);
    }
}
