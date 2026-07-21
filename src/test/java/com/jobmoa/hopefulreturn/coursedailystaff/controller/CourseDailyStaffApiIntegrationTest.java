package com.jobmoa.hopefulreturn.coursedailystaff.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.security.JwtTokenProvider;
import com.jobmoa.hopefulreturn.staffschedule.entity.StaffScheduleEntity;
import com.jobmoa.hopefulreturn.staffschedule.repository.StaffScheduleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
 * 회차 날짜별 인력 배정(CourseDailyStaff) API HTTP 통합 테스트 — 실제 SecurityConfig(@PreAuthorize)·
 * JwtAuthenticationFilter·GlobalExceptionHandler·JPA(실 DB)까지 전 구간을 MockMvc로 검증한다.
 * 이슈 #58(근무 불가일 차단·강사 AM/PM 세션 가용성·가용일 행 upsert)의 완료조건을 대상으로 한다.
 * 토큰은 실제 {@link JwtTokenProvider}로 발급(oper01=OPERATOR/6, staff01=STAFF/9 — V4 시드 기준).
 * course/staff_schedule 은 각 테스트에서 시드하며 @Transactional 로 롤백되어 DB를 오염시키지 않는다.
 *
 * 실행 조건: 실제 MSSQL 필요 → {@code DB_PASSWORD} 환경변수가 있을 때만 활성화(무DB CI 자동 스킵).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class CourseDailyStaffApiIntegrationTest {

    private static final String BASE = "/api/course-daily-staffs";
    private static final Long STAFF_USER_ID = 9L;   // staff01 → 역할 STAFF(배정 가능)
    private static final LocalDate DAY1 = LocalDate.of(2026, 9, 15);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private StaffScheduleRepository staffScheduleRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private String opToken;

    @BeforeEach
    void setUp() {
        opToken = jwtTokenProvider.createAccessToken(6L, "oper01", "OPERATOR");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Long seedCourse() {
        CourseEntity course = CourseEntity.builder()
                .regionId(1L) // 서울(V6 시드)
                .courseNumber(5)
                .localCourseNumber(1) // V7 NOT NULL
                .courseName("양천5기")
                .capacity(20)
                .minimumCapacity(5)
                .status(CourseStatus.PLANNED)
                .day1Date(DAY1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return courseRepository.saveAndFlush(course).getCourseId();
    }

    /** 근무 불가일(course_staff_id NULL·is_available=false) 시드. */
    private void seedUnavailable(Long userId, LocalDate date, SessionType session) {
        staffScheduleRepository.saveAndFlush(StaffScheduleEntity.builder()
                .userId(userId).scheduleDate(date).sessionType(session)
                .isAvailable(false).memo("불가 시드")
                .createdAt(LocalDateTime.now())
                .build());
    }

    /** 사용자 등록 가용일(course_staff_id NULL·is_available=true) 시드. */
    private Long seedAvailable(Long userId, LocalDate date, SessionType session) {
        return staffScheduleRepository.saveAndFlush(StaffScheduleEntity.builder()
                .userId(userId).scheduleDate(date).sessionType(session)
                .isAvailable(true).memo("가용 시드")
                .createdAt(LocalDateTime.now())
                .build()).getStaffScheduleId();
    }

    private String bulkBody(Long courseId, SessionType session) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "courseId", courseId,
                "entries", List.of(Map.of(
                        "scheduleDate", DAY1.toString(),
                        "staffRole", "STAFF",
                        "sessionType", session.name(),
                        "userId", STAFF_USER_ID))));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("[409] 근무 불가일(AM)에 AM 배정 저장 → ASSIGN_ON_UNAVAILABLE_DATE, 불가 목록 반환")
    void bulk_onUnavailableDate_conflict() throws Exception {
        Long courseId = seedCourse();
        seedUnavailable(STAFF_USER_ID, DAY1, SessionType.AM);
        flushAndClear();

        mockMvc.perform(put(BASE + "/bulk")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkBody(courseId, SessionType.AM)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("근무 불가일에는 배정할 수 없습니다."))
                .andExpect(jsonPath("$.data[0].userId").value(STAFF_USER_ID))
                .andExpect(jsonPath("$.data[0].sessionType").value("AM"));
    }

    @Test
    @DisplayName("[409] FULL 배정은 AM만 불가여도 세션이 겹쳐 거부된다")
    void bulk_fullOnAmUnavailable_conflict() throws Exception {
        Long courseId = seedCourse();
        seedUnavailable(STAFF_USER_ID, DAY1, SessionType.AM);
        flushAndClear();

        mockMvc.perform(put(BASE + "/bulk")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkBody(courseId, SessionType.FULL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("근무 불가일에는 배정할 수 없습니다."));
    }

    @Test
    @DisplayName("[200] AM만 불가로 등록한 인력은 후보 조회 시 그 날 availability가 PM으로만 내려온다")
    void candidates_sessionAware_amUnavailableShowsPmOnly() throws Exception {
        Long courseId = seedCourse();
        seedUnavailable(STAFF_USER_ID, DAY1, SessionType.AM);
        flushAndClear();

        mockMvc.perform(get(BASE + "/candidates")
                        .param("courseId", String.valueOf(courseId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // staff01(9) 후보의 DAY1 가용 세션은 PM(오전 불가 → 오후만)
                .andExpect(jsonPath("$.data.candidates[?(@.userId == " + STAFF_USER_ID + ")]"
                        + ".availability[?(@.scheduleDate == '" + DAY1 + "')].sessionType").value("PM"));
    }

    @Test
    @DisplayName("[200] 가용일 기등록(available) 행이 있으면 배정 시 UNIQUE 충돌 없이 기존 행을 update(중복 미생성)")
    void bulk_upsertsExistingAvailabilityRow() throws Exception {
        Long courseId = seedCourse();
        Long existingId = seedAvailable(STAFF_USER_ID, DAY1, SessionType.AM);
        flushAndClear();

        mockMvc.perform(put(BASE + "/bulk")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkBody(courseId, SessionType.AM)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(1));

        flushAndClear();
        // 동일 UNIQUE 키(user·date·session) 행은 여전히 기존 행이며, 배정 연결됨(course_staff_id NOT NULL)
        StaffScheduleEntity row = staffScheduleRepository
                .findByUserIdAndScheduleDateAndSessionType(STAFF_USER_ID, DAY1, SessionType.AM)
                .orElseThrow();
        assertThat(row.getStaffScheduleId()).isEqualTo(existingId); // 신규 아님 — 기존 행 재사용
        assertThat(row.getCourseStaffId()).isNotNull();             // 배정 연결됨
        assertThat(row.getIsAvailable()).isTrue();                  // 가용 유지
    }
}
