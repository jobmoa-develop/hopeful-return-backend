package com.jobmoa.hopefulreturn.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SMS 미연동 상태의 기본 구현. 추후 실제 SMS provider 구현으로 교체.
 */
@Slf4j
@Service
public class NoOpSmsService implements SmsService {

    @Override
    public void sendVerificationCode(String phoneNumber, String code) {
        log.info("[SMS-NOOP] to={}, code={}", phoneNumber, code);
    }
}
