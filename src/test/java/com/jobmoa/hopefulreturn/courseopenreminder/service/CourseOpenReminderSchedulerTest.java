package com.jobmoa.hopefulreturn.courseopenreminder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.courseopenreminder.entity.CourseOpenReminderConfigEntity;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.CourseOpenReminderRunResult;
import com.jobmoa.hopefulreturn.courseopenreminder.repository.CourseOpenReminderConfigRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 개강 하루전 자동 문자 스케줄러 가드 단위 테스트 —
 * 비활성/오늘 이미 실행/발송 시각 전이면 배치를 실행하지 않고, 조건 충족 시 실행 후 last_run_date 를 갱신한다.
 */
@ExtendWith(MockitoExtension.class)
class CourseOpenReminderSchedulerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 8);

    @Mock
    private CourseOpenReminderConfigRepository configRepository;
    @Mock
    private CourseOpenReminderSender sender;

    private CourseOpenReminderScheduler scheduler(LocalTime now) {
        Clock clock = Clock.fixed(TODAY.atTime(now).atZone(KST).toInstant(), KST);
        return new CourseOpenReminderScheduler(configRepository, sender, clock);
    }

    private CourseOpenReminderConfigEntity config(boolean enabled, LocalTime sendTime, LocalDate lastRun) {
        return CourseOpenReminderConfigEntity.builder()
                .reminderConfigId(1L).enabled(enabled).daysBefore(1)
                .sendTime(sendTime).lastRunDate(lastRun).build();
    }

    @Test
    @DisplayName("비활성이면 배치를 실행하지 않는다")
    void disabledSkips() {
        when(configRepository.findTopByOrderByReminderConfigIdAsc())
                .thenReturn(Optional.of(config(false, LocalTime.of(9, 0), null)));

        scheduler(LocalTime.of(10, 0)).runIfDue();

        verify(sender, never()).runBatch(any(), any());
        verify(configRepository, never()).save(any());
    }

    @Test
    @DisplayName("오늘 이미 실행했으면(last_run_date=today) 재실행하지 않는다")
    void alreadyRanTodaySkips() {
        when(configRepository.findTopByOrderByReminderConfigIdAsc())
                .thenReturn(Optional.of(config(true, LocalTime.of(9, 0), TODAY)));

        scheduler(LocalTime.of(10, 0)).runIfDue();

        verify(sender, never()).runBatch(any(), any());
        verify(configRepository, never()).save(any());
    }

    @Test
    @DisplayName("발송 시각 전이면 실행하지 않는다")
    void beforeSendTimeSkips() {
        when(configRepository.findTopByOrderByReminderConfigIdAsc())
                .thenReturn(Optional.of(config(true, LocalTime.of(9, 0), null)));

        scheduler(LocalTime.of(8, 59)).runIfDue();

        verify(sender, never()).runBatch(any(), any());
        verify(configRepository, never()).save(any());
    }

    @Test
    @DisplayName("활성·미실행·발송 시각 경과면 배치 실행 후 last_run_date 를 오늘로 갱신한다")
    void dueRunsAndStampsLastRunDate() {
        CourseOpenReminderConfigEntity config = config(true, LocalTime.of(9, 0), null);
        when(configRepository.findTopByOrderByReminderConfigIdAsc()).thenReturn(Optional.of(config));
        when(sender.runBatch(eq(TODAY), eq(config)))
                .thenReturn(new CourseOpenReminderRunResult(2, 3, 0, 1));

        scheduler(LocalTime.of(9, 0)).runIfDue();

        verify(sender).runBatch(TODAY, config);
        assertThat(config.getLastRunDate()).isEqualTo(TODAY);
        verify(configRepository).save(config);
    }
}
