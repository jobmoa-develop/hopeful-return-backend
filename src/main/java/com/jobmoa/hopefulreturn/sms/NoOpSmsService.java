package com.jobmoa.hopefulreturn.sms;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * SMS 미연동(폴백) 구현. sens.enabled=false(기본)일 때 활성 — 실발송 없이 로그만 남긴다.
 * 실연동은 {@link SensSmsService}(sens.enabled=true)로 교체된다.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "sens", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpSmsService implements SmsService {

    @Override
    public void sendVerificationCode(String phoneNumber, String code) {
        log.info("[SMS-NOOP] to={}, code={}", phoneNumber, code);
    }

    @Override
    public SmsSendResult send(SmsSendCommand command) {
        log.info("[SMS-NOOP] type={}, recipients={}, images={}",
                command.type(),
                command.recipients() == null ? 0 : command.recipients().size(),
                command.imagesBase64() == null ? 0 : command.imagesBase64().size());
        return SmsSendResult.ok("202", "success(noop)", null, List.of());
    }
}
