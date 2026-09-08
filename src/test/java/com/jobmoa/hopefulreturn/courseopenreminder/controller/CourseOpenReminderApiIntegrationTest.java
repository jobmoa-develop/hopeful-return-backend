package com.jobmoa.hopefulreturn.courseopenreminder.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseopenreminder.entity.CourseOpenReminderConfigEntity;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.UpdateCourseOpenReminderConfigRequest;
import com.jobmoa.hopefulreturn.courseopenreminder.repository.CourseOpenReminderConfigRepository;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.CourseStaffSmsEntity;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffNotifyType;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffSmsSendStatus;
import com.jobmoa.hopefulreturn.coursestaffsms.repository.CourseStaffSmsRepository;
import com.jobmoa.hopefulreturn.security.JwtTokenProvider;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개강 하루전 자동 문자 설정·수동발송 API HTTP 통합 테스트 —
 * SecurityConfig·JwtAuthenticationFilter(@RequestAttribute userId)·@Valid·GlobalExceptionHandler·JPA(실 DB)를
 * MockMvc 로 전 구간 검증한다. 토큰은 실제 {@link JwtTokenProvider} 로 발급(admin01/1 — V4 시드).
 * 설정은 V27 시드 단일 행을 사용하며, 모든 변경은 @Transactional 로 롤백된다.
 *
 * <p>권한: 설정 조회/수정 = ADMIN·HEAD_OFFICE, 즉시 발송(/run) = ADMIN.
 * 실행 조건: 실제 MSSQL 필요 → {@code DB_PASSWORD} 환경변수가 있을 때만 활성화(무DB CI 자동 스킵).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class CourseOpenReminderApiIntegrationTest {

    private static final String CONFIG_URL = "/api/course-open-reminder/config";
    private static final String RUN_URL = "/api/course-open-reminder/run";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseOpenReminderConfigRepository configRepository;
    @Autowired
    private CourseStaffSmsRepository courseStaffSmsRepository;
    @Autowired
    private Clock clock;

    private String adminToken;
    private String headOfficeToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtTokenProvider.createAccessToken(1L, "admin01", List.of("ADMIN"));
        headOfficeToken = jwtTokenProvider.createAccessToken(2L, "head01", List.of("HEAD_OFFICE"));
        staffToken = jwtTokenProvider.createAccessToken(50L, "staff01", List.of("STAFF"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    @DisplayName("GET /config — ADMIN 은 설정 조회에 성공한다")
    void getConfig_asAdmin_ok() throws Exception {
        mockMvc.perform(get(CONFIG_URL).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.daysBefore").isNumber())
                .andExpect(jsonPath("$.data.sendTime").value(org.hamcrest.Matchers.matchesPattern("\\d{2}:\\d{2}")));
    }

    @Test
    @DisplayName("PUT /config — ADMIN 수정 후 값이 반영되고 재조회에도 유지된다")
    void updateConfig_asAdmin_roundTrip() throws Exception {
        String body = objectMapper.writeValueAsString(
                new UpdateCourseOpenReminderConfigRequest(false, 3, LocalTime.of(8, 30)));

        mockMvc.perform(put(CONFIG_URL).header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.daysBefore").value(3))
                .andExpect(jsonPath("$.data.sendTime").value("08:30"));

        mockMvc.perform(get(CONFIG_URL).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.daysBefore").value(3))
                .andExpect(jsonPath("$.data.sendTime").value("08:30"));
    }

    @Test
    @DisplayName("PUT /config — daysBefore 범위 밖(0)이면 400")
    void updateConfig_invalidDaysBefore_400() throws Exception {
        String body = "{\"enabled\":true,\"daysBefore\":0,\"sendTime\":\"09:00\"}";

        mockMvc.perform(put(CONFIG_URL).header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /config — 발송 시각 누락이면 400")
    void updateConfig_missingSendTime_400() throws Exception {
        String body = "{\"enabled\":true,\"daysBefore\":1}";

        mockMvc.perform(put(CONFIG_URL).header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /config — 토큰 없으면 401")
    void getConfig_noToken_401() throws Exception {
        mockMvc.perform(get(CONFIG_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /config — 권한 없는 역할(STAFF)은 403")
    void getConfig_asStaff_403() throws Exception {
        mockMvc.perform(get(CONFIG_URL).header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /run — ADMIN: 엔드포인트 동작(대상 회차 집계) + COURSE_OPEN_REMINDER 이력 INSERT 허용")
    void run_asAdmin_endpointWorksAndReminderNotifyTypeInsertAllowed() throws Exception {
        // days_before=1 고정 + 내일 개강 CLOSED 회차 seed → sender 의 대상 조회에 이 회차가 잡힌다(targetCourses≥1).
        // (sent 건수는 배정 인력·dedup·공유 DB 상태에 좌우되므로 단정하지 않는다.)
        CourseOpenReminderConfigEntity config = configRepository.findTopByOrderByReminderConfigIdAsc().orElseThrow();
        config.setEnabled(true);
        config.setDaysBefore(1);
        config.setSendTime(LocalTime.of(9, 0));
        config.setLastRunDate(null);
        configRepository.saveAndFlush(config);

        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        CourseEntity course = courseRepository.saveAndFlush(CourseEntity.builder()
                .regionId(1L).courseNumber(9002).localCourseNumber(9002).courseName("개강문자통합테스트기")
                .capacity(20).minimumCapacity(5).status(CourseStatus.CLOSED).day1Date(tomorrow)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post(RUN_URL).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetCourses",
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.sent").isNumber())
                .andExpect(jsonPath("$.data.skipped").isNumber());

        // V28 회귀 가드: course_staff_sms 에 notify_type=COURSE_OPEN_REMINDER 로 INSERT 가 허용되어야 한다
        // (CHECK 제약 CK_COURSE_STAFF_SMS_NOTIFY_TYPE 이 신규 값을 막으면 여기서 예외로 실패한다). userId=1 은 admin01(V4 시드).
        CourseStaffSmsEntity saved = courseStaffSmsRepository.saveAndFlush(CourseStaffSmsEntity.builder()
                .courseId(course.getCourseId()).userId(1L).sentBy(null)
                .notifyType(StaffNotifyType.COURSE_OPEN_REMINDER)
                .content("개강 회차 하루전 안내(테스트)").sendStatus(StaffSmsSendStatus.SUCCESS)
                .sentAt(LocalDateTime.now()).createdAt(LocalDateTime.now())
                .build());
        assertThat(saved.getCourseStaffSmsId()).isNotNull();
    }

    @Test
    @DisplayName("POST /run — HEAD_OFFICE 는 권한 없음(403)")
    void run_asHeadOffice_403() throws Exception {
        mockMvc.perform(post(RUN_URL).header(HttpHeaders.AUTHORIZATION, bearer(headOfficeToken)))
                .andExpect(status().isForbidden());
    }
}
