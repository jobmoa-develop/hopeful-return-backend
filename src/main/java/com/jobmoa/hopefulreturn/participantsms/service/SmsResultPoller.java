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
        // 승격과 결과폴링은 관심사가 달라 각각 방어 — 하나가 실패해도 다른 하나는 진행한다.
        try {
            // 예약시각이 도래한 RESERVED → PENDING 승격 먼저(승격분은 아래 결과폴링이 곧바로 이어받는다).
            participantSmsService.promoteDueReservations();
        } catch (RuntimeException e) {
            log.warn("[SMS] 예약 발송 승격 중 오류", e);
        }
        try {
            participantSmsService.pollPendingResults();
        } catch (RuntimeException e) {
            // 스케줄러 스레드가 예외로 중단되지 않도록 방어(다음 주기에 재시도).
            log.warn("[SMS] 발송결과 폴링 중 오류", e);
        }
    }
}
