package com.jobmoa.hopefulreturn.counselingschedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.counselingschedule.model.dto.CounselingScheduleResponse;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselingType;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.region.support.RegionResolver;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CounselingScheduleServiceImplTest {

    @Mock
    private CourseParticipantCounselorRepository counselorRepository;
    @Mock
    private RegionResolver regionResolver;

    @InjectMocks
    private CounselingScheduleServiceImpl service;

    private CourseParticipantCounselorEntity row() {
        RegionEntity region = RegionEntity.builder().regionId(2L).name("양천").build();
        CourseEntity course = CourseEntity.builder()
                .courseId(15L).regionId(2L).courseNumber(3).region(region).build();
        ParticipantEntity participant = ParticipantEntity.builder()
                .participantId(25L).name("홍길동").build();
        CourseParticipantEntity cp = CourseParticipantEntity.builder()
                .courseParticipantId(101L).courseId(15L).participantId(25L)
                .course(course).participant(participant).build();
        UsersEntity counselor = UsersEntity.builder().userId(7L).name("상담사1").build();
        return CourseParticipantCounselorEntity.builder()
                .courseParticipantCounselorId(500L)
                .courseParticipantId(101L)
                .counselorId(7L)
                .status(CounselingType.PRE_SESSION)
                .counselingStartedAt(LocalDateTime.of(2026, 8, 10, 14, 0))
                .counselingEndedAt(LocalDateTime.of(2026, 8, 10, 15, 0))
                .counselor(counselor)
                .courseParticipant(cp)
                .build();
    }

    @Test
    @DisplayName("from/to 를 [from 자정, to+1일 자정) 범위로 변환하고 지역(단일)·회차 필터와 함께 결과를 매핑한다")
    void findSchedules_convertsRangeAndMaps() {
        when(regionResolver.resolveRegionIds(2L, null)).thenReturn(List.of(2L));
        when(counselorRepository.findCounselingSchedules(
                eq(LocalDateTime.of(2026, 8, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 9, 1, 0, 0)),
                eq(List.of(2L)),
                eq(3),
                isNull(),
                isNull()))
                .thenReturn(List.of(row()));

        CounselingScheduleResponse response = service.findSchedules(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), 2L, null, 3, null, "   ");

        assertThat(response.schedules()).hasSize(1);
        CounselingScheduleResponse.Item item = response.schedules().get(0);
        assertThat(item.date()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(item.regionName()).isEqualTo("양천");
        assertThat(item.courseNumber()).isEqualTo(3);
        assertThat(item.participantName()).isEqualTo("홍길동");
        assertThat(item.counselorName()).isEqualTo("상담사1");
        assertThat(item.counselingType()).isEqualTo("PRE_SESSION");
        assertThat(item.completed()).isTrue();
    }

    @Test
    @DisplayName("상담사명은 trim 후 %검색어% LIKE 패턴으로 전달하고, 빈 문자열은 null 로 전달한다")
    void findSchedules_buildsCounselorPattern() {
        when(regionResolver.resolveRegionIds(null, null)).thenReturn(null);
        when(counselorRepository.findCounselingSchedules(any(), any(), isNull(), isNull(), isNull(), eq("%김상담%")))
                .thenReturn(List.of());

        service.findSchedules(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 7), null, null, null, null, "  김상담 ");

        verify(counselorRepository).findCounselingSchedules(
                eq(LocalDateTime.of(2026, 8, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 8, 8, 0, 0)),
                isNull(),
                isNull(),
                isNull(),
                eq("%김상담%"));
    }

    @Test
    @DisplayName("상위 지역인데 산하 하위 지역이 없으면 조회 없이 0건을 반환한다")
    void findSchedules_emptyParentRegion_shortCircuits() {
        when(regionResolver.resolveRegionIds(null, 99L)).thenReturn(List.of());

        CounselingScheduleResponse response = service.findSchedules(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, 99L, null, null, null);

        assertThat(response.schedules()).isEmpty();
        verify(counselorRepository, never()).findCounselingSchedules(any(), any(), any(), any(), any(), any());
    }
}
