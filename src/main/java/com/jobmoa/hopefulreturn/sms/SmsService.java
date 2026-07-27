package com.jobmoa.hopefulreturn.sms;

/**
 * SMS 발송 인터페이스. 실제 구현(NHN Cloud, CoolSMS 등)은 추후 추가.
 */
public interface SmsService {

    void sendVerificationCode(String phoneNumber, String code);

    /**
     * 문자(SMS/LMS/MMS) 발송. recipients 는 SENS 한도(100건) 이하로 호출자가 분할해 전달한다.
     */
    SmsSendResult send(SmsSendCommand command);
}
