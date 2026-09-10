package com.jobmoa.hopefulreturn.calendarfeed.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.jobmoa.hopefulreturn.calendarfeed.model.dto.CalendarFeedStatusResponse;
import com.jobmoa.hopefulreturn.calendarfeed.service.CalendarFeedService;
import java.util.Optional;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * ICS 피드 컨트롤러 라우팅 테스트(standalone). {token}.ics 경로 매핑·text/calendar·404,
 * 그리고 관리 엔드포인트의 userId 주입을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CalendarFeedControllerTest {

    @Mock
    private CalendarFeedService calendarFeedService;

    @InjectMocks
    private CalendarFeedController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /{token}.ics: 유효 토큰이면 200 + text/calendar 본문")
    void feedReturnsIcs() throws Exception {
        when(calendarFeedService.buildIcsFeed("abc123"))
                .thenReturn(Optional.of("BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n"));

        mockMvc.perform(get("/api/calendar/feed/abc123.ics"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/calendar"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BEGIN:VCALENDAR")));

        verify(calendarFeedService).buildIcsFeed("abc123");
    }

    @Test
    @DisplayName("GET /{token}.ics: 알 수 없는 토큰이면 404")
    void feedReturns404ForUnknownToken() throws Exception {
        when(calendarFeedService.buildIcsFeed("bad")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/calendar/feed/bad.ics"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/calendar/feed: 상태 조회(userId 주입)")
    void status() throws Exception {
        when(calendarFeedService.getStatus(6L))
                .thenReturn(new CalendarFeedStatusResponse(false, null));

        mockMvc.perform(get("/api/calendar/feed").requestAttr("userId", 6L))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @DisplayName("POST /api/calendar/feed: 연동 활성화")
    void enable() throws Exception {
        when(calendarFeedService.enable(6L))
                .thenReturn(new CalendarFeedStatusResponse(true, "http://localhost:3434/api/calendar/feed/t.ics"));

        mockMvc.perform(post("/api/calendar/feed").requestAttr("userId", 6L))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.feedUrl").value("http://localhost:3434/api/calendar/feed/t.ics"));
    }

    @Test
    @DisplayName("DELETE /api/calendar/feed: 연동 해제 후 상태 반환")
    void disable() throws Exception {
        when(calendarFeedService.getStatus(6L)).thenReturn(new CalendarFeedStatusResponse(false, null));

        mockMvc.perform(delete("/api/calendar/feed").requestAttr("userId", 6L))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        verify(calendarFeedService).disable(eq(6L));
    }
}
