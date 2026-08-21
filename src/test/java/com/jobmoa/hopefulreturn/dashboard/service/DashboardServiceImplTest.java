package com.jobmoa.hopefulreturn.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessDayCalculator;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.dashboard.model.dto.DashboardCalendarResponse;
import com.jobmoa.hopefulreturn.dashboard.repository.DashboardTaskCompletionRepository;
import com.jobmoa.hopefulreturn.followup.repository.FollowUpRepository;
import com.jobmoa.hopefulreturn.followupcounsel.repository.FollowUpCounselRepository;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 대시보드 캘린더 파생 마감 항목(교육 종료일 기준 영업일) 단위 테스트.
 * 조회 대상 월을 먼 미래(2099-01)로 잡아 "오늘 경고 항목" 병합을 배제하고 마감 항목만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final LocalDate LAST_EDUCATION_DAY = LocalDate.of(2099, 1, 1);

    @Mock
    private RegionRepository regionRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseParticipantRepository courseParticipantRepository;
    @Mock
    private FollowUpRepository followUpRepository;
    @Mock
    private FollowUpCounselRepository followUpCounselRepository;
    @Mock
    private DashboardTaskCompletionRepository dashboardTaskCompletionRepository;

    @InjectMocks
    private DashboardServiceImpl service;

    private CourseEntity courseEndingAt(LocalDate lastDay) {
        return CourseEntity.builder()
                .courseId(1L)
                .localCourseNumber(3)
                .region(RegionEntity.builder().name("서울").build())
                .day1Date(lastDay)
                .build();
    }

    @Test
    @DisplayName("참여자 수당 지급 마감은 교육 종료일 + 영업일 7일에 생성된다")
    void getCalendar_buildsAllowanceDeadline() {
        LocalDate deadline = BusinessDayCalculator.addBusinessDays(LAST_EDUCATION_DAY, 7);
        stubEmptyTaskDates();
        when(courseRepository.findAll()).thenReturn(List.of(courseEndingAt(LAST_EDUCATION_DAY)));

        DashboardCalendarResponse.Item allowance =
                findTask(service.getCalendar(deadline.getYear(), deadline.getMonthValue()), "ALLOWANCE_PAYMENT");

        assertThat(allowance.date()).isEqualTo(deadline);
        assertThat(allowance.title()).contains("참여자 수당 지급").contains("서울 3회차");
    }

    @Test
    @DisplayName("수행결과보고서 제출 마감은 교육 종료일 + 영업일 21일에 생성되고 라벨이 통일된다")
    void getCalendar_buildsReportDeadlineWithUnifiedLabel() {
        LocalDate deadline = BusinessDayCalculator.addBusinessDays(LAST_EDUCATION_DAY, 21);
        stubEmptyTaskDates();
        when(courseRepository.findAll()).thenReturn(List.of(courseEndingAt(LAST_EDUCATION_DAY)));

        DashboardCalendarResponse.Item report =
                findTask(service.getCalendar(deadline.getYear(), deadline.getMonthValue()), "REPORT_SUBMIT");

        assertThat(report.date()).isEqualTo(deadline);
        assertThat(report.title()).contains("수행결과보고서 제출");
    }

    private void stubEmptyTaskDates() {
        when(courseRepository.findByRecruitStartBetween(any(), any())).thenReturn(List.of());
        when(courseRepository.findByRecruitEndBetween(any(), any())).thenReturn(List.of());
        when(courseRepository.findByPlanSubmitDateBetween(any(), any())).thenReturn(List.of());
    }

    private DashboardCalendarResponse.Item findTask(DashboardCalendarResponse response, String taskType) {
        return response.content().stream()
                .filter(i -> taskType.equals(i.taskType()))
                .findFirst()
                .orElseThrow();
    }
}
