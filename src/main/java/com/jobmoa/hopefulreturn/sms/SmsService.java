package com.jobmoa.hopefulreturn.sms;

import java.util.List;

/**
 * SMS 발송 인터페이스. 실제 구현(NHN Cloud, CoolSMS 등)은 추후 추가.
 */
public interface SmsService {

    void sendVerificationCode(String phoneNumber, String code);

    /**
     * 문자(SMS/LMS/MMS) 발송. recipients 는 SENS 한도(100건) 이하로 호출자가 분할해 전달한다.
     */
    SmsSendResult send(SmsSendCommand command);

    /**
     * 발송 요청(requestId)에 대한 수신 건별 발송결과 조회. 미연동(NoOp) 구현은 빈 목록을 반환한다.
     * 조회 실패 시 예외 대신 빈 목록/PENDING 으로 관대하게 처리하여 폴러가 재시도할 수 있게 한다.
     */
    List<SmsMessageResult> lookupResults(String requestId);
}
