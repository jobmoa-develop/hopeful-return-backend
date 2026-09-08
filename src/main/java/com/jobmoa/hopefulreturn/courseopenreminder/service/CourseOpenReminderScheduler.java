package com.jobmoa.hopefulreturn.courseopenreminder.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.courseopenreminder.entity.CourseOpenReminderConfigEntity;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.CourseOpenReminderRunResult;
import com.jobmoa.hopefulreturn.courseopenreminder.repository.CourseOpenReminderConfigRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 개강 하루전 자동 문자 스케줄러. 기본 10분마다 틱하여, 설정된 발송 시각(send_time) 이후 하루 1회
 * 배치를 실행한다. {@code last_run_date} 로 하루 1회 실행을 보장하며, 이 값은 이 스케줄러만 읽고 쓴다
 * (다른 기능과 공유 상태 없음). 배치의 실제 발송은 {@link CourseOpenReminderSender} 에 위임한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseOpenReminderScheduler {

    private final CourseOpenReminderConfigRepository configRepository;
    private final CourseOpenReminderSender sender;
    private final Clock clock;

    @Scheduled(cron = "${course-open-reminder.check-cron:0 */10 * * * *}")
    public void tick() {
        try {
            runIfDue();
        } catch (RuntimeException e) {
            // 스케줄러 스레드가 예외로 중단되지 않도록 방어(다음 주기에 재시도).
            log.warn("[OpenReminder] 스케줄 실행 중 오류", e);
        }
    }

    /**
     * 설정이 활성이고 오늘 아직 실행하지 않았으며 발송 시각이 지났다면 배치를 실행하고 last_run_date 를 갱신한다.
     *
     * <p>스케줄러는 단일 인스턴스·단일 스레드로 동작(기본 TaskScheduler)해 같은 잡이 겹쳐 실행되지 않는다.
     * 만약 다중 인스턴스 등으로 중복 실행되더라도, 발송 직전 이력 dedup
     * ({@code existsBy...SentAtAfter})이 같은 날 재발송을 막는 안전망이다.
     * last_run_date 는 배치가 정상 완료된 뒤 오늘로 찍혀 "하루 1회" 를 보장한다(발송 0건이어도 완료로 간주).
     */
    void runIfDue() {
        CourseOpenReminderConfigEntity config =
                configRepository.findTopByOrderByReminderConfigIdAsc().orElse(null);
        if (config == null || !config.isEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        if (today.equals(config.getLastRunDate())) {
            return; // 오늘 이미 실행
        }
        if (now.toLocalTime().isBefore(config.getSendTime())) {
            return; // 아직 발송 시각 전
        }
        CourseOpenReminderRunResult result = sender.runBatch(today, config);
        config.setLastRunDate(today);
        configRepository.save(config);
        log.info("[OpenReminder] 자동 발송 완료 today={} result={}", today, result);
    }

    /**
     * 수동 실행(운영·테스트). 발송 시각·last_run_date·활성 게이트를 무시하고 즉시 배치를 실행한다.
     * 중복 발송 방지(이력 기반 dedup)는 유지된다.
     */
    public CourseOpenReminderRunResult runNow() {
        CourseOpenReminderConfigEntity config = configRepository.findTopByOrderByReminderConfigIdAsc()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "개강 문자 발송 설정이 없습니다."));
        LocalDate today = LocalDate.now(clock);
        CourseOpenReminderRunResult result = sender.runBatch(today, config);
        log.info("[OpenReminder] 수동 발송 today={} result={}", today, result);
        return result;
    }
}
