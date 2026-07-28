package com.jobmoa.hopefulreturn.participantsms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SENS 발송결과 조회 폴러. sens.enabled=true 일 때만 활성(미연동 모드는 PENDING 이 없어 불필요).
 * 스케줄링만 담당하고 실제 조회·갱신 로직은 {@link ParticipantSmsService#pollPendingResults()} 에 위임한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sens", name = "enabled", havingValue = "true")
public class SmsResultPoller {

    private final ParticipantSmsService participantSmsService;

    @Scheduled(fixedDelayString = "${sens.result-poll.interval-ms:60000}")
    public void poll() {
        try {
            participantSmsService.pollPendingResults();
        } catch (RuntimeException e) {
            // 스케줄러 스레드가 예외로 중단되지 않도록 방어(다음 주기에 재시도).
            log.warn("[SMS] 발송결과 폴링 중 오류", e);
        }
    }
}
