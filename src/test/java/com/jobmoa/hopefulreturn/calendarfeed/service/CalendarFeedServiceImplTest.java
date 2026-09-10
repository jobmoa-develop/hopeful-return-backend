package com.jobmoa.hopefulreturn.calendarfeed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.calendarfeed.model.dto.CalendarFeedStatusResponse;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.StaffScheduleListResponse;
import com.jobmoa.hopefulreturn.staffschedule.service.StaffScheduleService;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * ICS 피드 서비스 단위 테스트. 리포지토리·일정 서비스를 목킹해 토큰 발급/해제/상태,
 * 미존재 토큰 처리, staff행·counselor합성행의 ICS 이벤트 매핑(코스 시각 우선·세션 폴백)을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CalendarFeedServiceImplTest {

    private static final Long USER_ID = 6L;
    private static final String BASE_URL = "http://localhost:3434";

    @Mock
    private UsersRepository usersRepository;
    @Mock
    private StaffScheduleService staffScheduleService;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CalendarFeedServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", BASE_URL);
    }

    private UsersEntity user(String token) {
        UsersEntity u = new UsersEntity();
        u.setUserId(USER_ID);
        u.setName("홍길동");
        u.setCalendarFeedToken(token);
        return u;
    }

    @Test
    @DisplayName("enable: 토큰을 발급하고 .ics 피드 URL 을 반환한다")
    void enableGeneratesTokenAndUrl() {
        UsersEntity u = user(null);
        when(usersRepository.findByUserIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(u));

        CalendarFeedStatusResponse res = service.enable(USER_ID);

        assertThat(res.enabled()).isTrue();
        assertThat(u.getCalendarFeedToken()).isNotBlank();
        assertThat(res.feedUrl())
                .startsWith(BASE_URL + "/api/calendar/feed/")
                .endsWith(".ics")
                .contains(u.getCalendarFeedToken());
        verify(usersRepository).save(u);
    }

    @Test
    @DisplayName("disable: 토큰을 제거한다")
    void disableClearsToken() {
        UsersEntity u = user("existing-token");
        when(usersRepository.findByUserIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(u));

        service.disable(USER_ID);

        assertThat(u.getCalendarFeedToken()).isNull();
        verify(usersRepository).save(u);
    }

    @Test
    @DisplayName("getStatus: 토큰이 없으면 미연동(enabled=false, url=null)")
    void statusWhenNotConnected() {
        when(usersRepository.findByUserIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(user(null)));

        CalendarFeedStatusResponse res = service.getStatus(USER_ID);

        assertThat(res.enabled()).isFalse();
        assertThat(res.feedUrl()).isNull();
    }

    @Test
    @DisplayName("enable: 사용자가 없으면 BusinessException")
    void enableThrowsWhenUserMissing() {
        when(usersRepository.findByUserIdAndDeletedFalse(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enable(USER_ID)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("buildIcsFeed: 알 수 없는 토큰이면 empty")
    void buildReturnsEmptyForUnknownToken() {
        when(usersRepository.findByCalendarFeedTokenAndDeletedFalse("nope")).thenReturn(Optional.empty());

        assertThat(service.buildIcsFeed("nope")).isEmpty();
    }

    @Test
    @DisplayName("buildIcsFeed: 가용성 블록은 세션 시각, 배정 회차는 코스 시각·장소로 이벤트를 만든다")
    void buildMapsStaffAndCounselorRows() {
        UsersEntity u = user("tok");
        when(usersRepository.findByCalendarFeedTokenAndDeletedFalse("tok")).thenReturn(Optional.of(u));

        // 1) 가용성 불가 블록(코스 없음 → AM 세션 매핑), 2) 상담사 합성 배정행(코스 시각·장소 사용)
        StaffScheduleListResponse.Item availability = new StaffScheduleListResponse.Item(
                1L, USER_ID, "홍길동", LocalDate.of(2026, 9, 20), "AM", false,
                null, null, null, null, null, "병가");
        StaffScheduleListResponse.Item assigned = new StaffScheduleListResponse.Item(
                null, USER_ID, "홍길동", LocalDate.of(2026, 9, 25), "FULL", true,
                77L, 18L, "서울 3회차", "IN_PROGRESS", "COUNSELOR", null);
        when(staffScheduleService.findMy(eq(USER_ID), any(), any()))
                .thenReturn(new StaffScheduleListResponse(List.of(availability, assigned), 0, 2, 2, 1));

        CourseEntity course = CourseEntity.builder()
                .courseId(18L)
                .educationStartTime(LocalTime.of(9, 30))
                .educationEndTime(LocalTime.of(17, 30))
                .location("서울시 강남")
                .build();
        when(courseRepository.findAllById(any())).thenReturn(List.of(course));

        String ics = service.buildIcsFeed("tok").orElseThrow();

        // 가용성 불가 블록
        assertThat(ics).contains("UID:ss-1@hopeful-return");
        assertThat(ics).contains("SUMMARY:근무 불가");
        assertThat(ics).contains("DTSTART;TZID=Asia/Seoul:20260920T090000");
        assertThat(ics).contains("DTEND;TZID=Asia/Seoul:20260920T130000");
        // 배정 상담 회차(코스 시각·장소 우선)
        assertThat(ics).contains("UID:cdc-77-2026-09-25@hopeful-return");
        assertThat(ics).contains("SUMMARY:서울 3회차 · 상담");
        assertThat(ics).contains("DTSTART;TZID=Asia/Seoul:20260925T093000");
        assertThat(ics).contains("DTEND;TZID=Asia/Seoul:20260925T173000");
        assertThat(ics).contains("LOCATION:서울시 강남");
    }

    @Test
    @DisplayName("buildIcsFeed: 가용성 블록만 있어도(코스 없음) NPE 없이 세션 시각으로 렌더링한다")
    void buildHandlesAvailabilityOnlyWithoutCourse() {
        UsersEntity u = user("tok");
        when(usersRepository.findByCalendarFeedTokenAndDeletedFalse("tok")).thenReturn(Optional.of(u));

        // courseId 가 없는 가용성 블록만 → courseIds 비어 courseRepository 미조회(불변맵 경로)
        StaffScheduleListResponse.Item availability = new StaffScheduleListResponse.Item(
                1L, USER_ID, "홍길동", LocalDate.of(2026, 9, 20), "PM", true,
                null, null, null, null, null, null);
        when(staffScheduleService.findMy(eq(USER_ID), any(), any()))
                .thenReturn(new StaffScheduleListResponse(List.of(availability), 0, 1, 1, 1));

        String ics = service.buildIcsFeed("tok").orElseThrow();

        assertThat(ics).contains("UID:ss-1@hopeful-return");
        assertThat(ics).contains("SUMMARY:근무 가능");
        assertThat(ics).contains("DTSTART;TZID=Asia/Seoul:20260920T130000");
        assertThat(ics).contains("DTEND;TZID=Asia/Seoul:20260920T180000");
    }
}
