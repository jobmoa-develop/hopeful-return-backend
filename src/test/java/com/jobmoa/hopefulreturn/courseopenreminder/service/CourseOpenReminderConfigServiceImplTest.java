package com.jobmoa.hopefulreturn.courseopenreminder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.courseopenreminder.entity.CourseOpenReminderConfigEntity;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.CourseOpenReminderConfigResponse;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.UpdateCourseOpenReminderConfigRequest;
import com.jobmoa.hopefulreturn.courseopenreminder.repository.CourseOpenReminderConfigRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 개강 하루전 자동 문자 설정 서비스 단위 테스트 —
 * get 매핑·update 필드/수정시각(clock)/수정자 반영·설정 부재 시 예외를 검증(무DB, 고정 Clock).
 */
@ExtendWith(MockitoExtension.class)
class CourseOpenReminderConfigServiceImplTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 8, 10, 0);
    private static final long UPDATER_ID = 7L;

    @Mock
    private CourseOpenReminderConfigRepository configRepository;

    private CourseOpenReminderConfigServiceImpl service() {
        Clock clock = Clock.fixed(NOW.atZone(KST).toInstant(), KST);
        return new CourseOpenReminderConfigServiceImpl(configRepository, clock);
    }

    private CourseOpenReminderConfigEntity seeded() {
        return CourseOpenReminderConfigEntity.builder()
                .reminderConfigId(1L).enabled(true).daysBefore(1)
                .sendTime(LocalTime.of(9, 0)).lastRunDate(LocalDate.of(2026, 9, 7))
                .build();
    }

    @Test
    @DisplayName("get 은 저장된 설정을 그대로 매핑해 반환한다")
    void getMapsConfig() {
        when(configRepository.findTopByOrderByReminderConfigIdAsc()).thenReturn(Optional.of(seeded()));

        CourseOpenReminderConfigResponse response = service().get();

        assertThat(response.enabled()).isTrue();
        assertThat(response.daysBefore()).isEqualTo(1);
        assertThat(response.sendTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.lastRunDate()).isEqualTo(LocalDate.of(2026, 9, 7));
    }

    @Test
    @DisplayName("update 는 값·수정시각(clock)·수정자를 반영해 저장하고 반환한다")
    void updateAppliesFieldsAndAudit() {
        CourseOpenReminderConfigEntity config = seeded();
        when(configRepository.findTopByOrderByReminderConfigIdAsc()).thenReturn(Optional.of(config));
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CourseOpenReminderConfigResponse response = service().update(
                new UpdateCourseOpenReminderConfigRequest(false, 3, LocalTime.of(8, 30)), UPDATER_ID);

        assertThat(response.enabled()).isFalse();
        assertThat(response.daysBefore()).isEqualTo(3);
        assertThat(response.sendTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(config.getUpdatedAt()).isEqualTo(NOW);
        assertThat(config.getUpdatedBy()).isEqualTo(UPDATER_ID);
        verify(configRepository).save(config);
    }

    @Test
    @DisplayName("설정 행이 없으면 get·update 모두 예외를 던진다")
    void throwsWhenConfigMissing() {
        when(configRepository.findTopByOrderByReminderConfigIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get()).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service().update(
                new UpdateCourseOpenReminderConfigRequest(true, 1, LocalTime.of(9, 0)), UPDATER_ID))
                .isInstanceOf(BusinessException.class);
    }
}
