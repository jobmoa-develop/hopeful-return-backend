package com.jobmoa.hopefulreturn.sms;

/**
 * SMS 발송 인터페이스. 실제 구현(NHN Cloud, CoolSMS 등)은 추후 추가.
 */
public interface SmsService {

    void sendVerificationCode(String phoneNumber, String code);
}
