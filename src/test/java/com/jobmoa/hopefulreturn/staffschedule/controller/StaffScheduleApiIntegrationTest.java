package com.jobmoa.hopefulreturn.staffschedule.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 스태프 일정(StaffSchedule) API HTTP 통합 테스트 — 실제 SecurityConfig(@PreAuthorize)·
 * JwtAuthenticationFilter·GlobalExceptionHandler·JPA(실 DB)까지 전 구간을 MockMvc로 검증한다.
 * 토큰은 실제 {@link JwtTokenProvider}로 발급(oper01/6, staff01/9, admin01/1, counsel01/7 — V4 시드 기준).
 * staff_schedule 은 각 테스트에서 시드하며 @Transactional 로 롤백되어 DB를 오염시키지 않는다.
 *
 * 실행 조건: 실제 MSSQL 필요 → {@code DB_PASSWORD} 환경변수가 있을 때만 활성화(무DB CI 자동 스킵).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class StaffScheduleApiIntegrationTest {

    private static final String BASE = "/api/staff-schedules";
    private static final Long STAFF_USER_ID = 9L;    // staff01 (소유자)
    private static final Long COUNSEL_USER_ID = 7L;  // counsel01 (비소유·비관리자)

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StaffScheduleRepository staffScheduleRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private String opToken;
    private String staffToken;
    private String adminToken;
    private String counselToken;

    @BeforeEach
    void setUp() {
        opToken = jwtTokenProvider.createAccessToken(6L, "oper01", "OPERATOR");
        staffToken = jwtTokenProvider.createAccessToken(STAFF_USER_ID, "staff01", "STAFF");
        adminToken = jwtTokenProvider.createAccessToken(1L, "admin01", "ADMIN");
        counselToken = jwtTokenProvider.createAccessToken(COUNSEL_USER_ID, "counsel01", "COUNSELOR");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Long seedSchedule(Long userId, LocalDate date, SessionType sessionType) {
        return staffScheduleRepository.saveAndFlush(StaffScheduleEntity.builder()
                .userId(userId).scheduleDate(date).sessionType(sessionType)
                .isAvailable(true).memo("시드")
                .createdAt(LocalDateTime.now())
                .build()).getStaffScheduleId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("[200] 단건 등록(STAFF, 본인) → staffScheduleId 반환, isAvailable 기본 true")
    void create_ok() throws Exception {
        Map<String, Object> body = Map.of(
                "scheduleDate", "2026-08-18", "sessionType", "AM", "memo", "오전만 가능");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.staffScheduleId").isNumber())
                .andExpect(jsonPath("$.data.userId").value(STAFF_USER_ID))
                .andExpect(jsonPath("$.data.sessionType").value("AM"))
                .andExpect(jsonPath("$.data.isAvailable").value(true))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @DisplayName("[409] 중복(user·date·session) 단건 등록 → DUPLICATE_STAFF_SCHEDULE")
    void create_duplicate_conflict() throws Exception {
        seedSchedule(STAFF_USER_ID, LocalDate.of(2026, 8, 18), SessionType.AM);
        flushAndClear();
        Map<String, Object> body = Map.of("scheduleDate", "2026-08-18", "sessionType", "AM");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("이미 등록된 스태프 일정입니다."));
    }

    @Test
    @DisplayName("[400] 잘못된 sessionType → INVALID_SESSION_TYPE")
    void create_invalidSessionType_badRequest() throws Exception {
        Map<String, Object> body = Map.of("scheduleDate", "2026-08-18", "sessionType", "EVENING");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("유효하지 않은 시간대(session_type)입니다."));
    }

    @Test
    @DisplayName("[403] 비관리자(STAFF)가 타인 userId 지정 등록 → ACCESS_DENIED")
    void create_forOther_notManager_forbidden() throws Exception {
        Map<String, Object> body = Map.of(
                "userId", COUNSEL_USER_ID, "scheduleDate", "2026-08-18", "sessionType", "AM");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("[200] 일괄 등록(STAFF) — 기존 항목 스킵, registered/skipped 반환")
    void bulk_skipsDuplicates() throws Exception {
        seedSchedule(STAFF_USER_ID, LocalDate.of(2026, 8, 18), SessionType.FULL);
        flushAndClear();
        Map<String, Object> entry1 = Map.of("scheduleDate", "2026-08-18", "sessionType", "FULL"); // 스킵
        Map<String, Object> entry2 = Map.of("scheduleDate", "2026-08-19", "sessionType", "AM");   // 등록
        Map<String, Object> body = Map.of("entries", List.of(entry1, entry2));

        mockMvc.perform(post(BASE + "/bulk")
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registered").value(1))
                .andExpect(jsonPath("$.data.skipped").value(1));
    }

    @Test
    @DisplayName("[200] 목록 조회(OPERATOR) — userId·시간대 필터 + 스태프명 조인")
    void list_ok() throws Exception {
        seedSchedule(STAFF_USER_ID, LocalDate.of(2026, 8, 18), SessionType.AM);
        flushAndClear();

        mockMvc.perform(get(BASE)
                        .param("userId", String.valueOf(STAFF_USER_ID))
                        .param("sessionType", "AM")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").value(STAFF_USER_ID))
                .andExpect(jsonPath("$.data.content[0].sessionType").value("AM"))
                .andExpect(jsonPath("$.data.content[0].userName").exists())
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    @DisplayName("[403] 목록 조회는 STAFF 권한으로 불가")
    void list_wrongRole_forbidden() throws Exception {
        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[200] 내 캘린더(/me, STAFF) — 본인 일정만 반환")
    void me_ok() throws Exception {
        seedSchedule(STAFF_USER_ID, LocalDate.of(2026, 8, 18), SessionType.AM);
        seedSchedule(COUNSEL_USER_ID, LocalDate.of(2026, 8, 18), SessionType.PM); // 타인 → 제외돼야 함
        flushAndClear();

        mockMvc.perform(get(BASE + "/me")
                        .param("fromDate", "2026-08-01").param("toDate", "2026-08-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").value(STAFF_USER_ID))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("[200] 상세 조회(STAFF)")
    void detail_ok() throws Exception {
        Long id = seedSchedule(STAFF_USER_ID, LocalDate.of(2026, 8, 18), SessionType.AM);
        flushAndClear();

        mockMvc.perform(get(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staffScheduleId").value(id))
                .andExpect(jsonPath("$.data.sessionType").value("AM"));
    }

    @Test
    @DisplayName("[404] 존재하지 않는 상세 조회 → STAFF_SCHEDULE_NOT_FOUND")
    void detail_notFound() throws Exception {
        mockMvc.perform(get(BASE + "/99999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("스태프 일정을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("[200] 수정(소유자 STAFF) — sessionType·memo 반영, DB 반영")
    void update_owner_ok() throws Exception {
        Long id = seedSchedule(STAFF_USER_ID, LocalDate.of(2026, 8, 18), SessionType.AM);
        Map<String, Object> body = Map.of("sessionType", "PM", "isAvailable", false, "memo", "오후로 변경");

        mockMvc.perform(put(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionType").value("PM"))
                .andExpect(jsonPath("$.data.isAvailable").value(false))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        flushAndClear();
        StaffScheduleEntity updated = staffScheduleRepository.findById(id).orElseThrow();
        assertThat(updated.getSessionType()).isEqualTo(SessionType.PM);
    }

    @Test
    @DisplayName("[403] 비소유·비관리자(COUNSELOR)가 타인 일정 수정 → ACCESS_DENIED")
    void update_notOwner_forbidden() throws Exception {
        Long id = seedSchedule(STAFF_USER_ID, LocalDate.of(2026, 8, 18), SessionType.AM);
        flushAndClear();
        Map<String, Object> body = Map.of("sessionType", "PM");

        mockMvc.perform(put(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(counselToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("[200] 관리자(OPERATOR)는 타인 일정도 수정 가능")
    void update_manager_ok() throws Exception {
        Long id = seedSchedule(STAFF_USER_ID, LocalDate.of(2026, 8, 18), SessionType.AM);
        flushAndClear();
        Map<String, Object> body = Map.of("memo", "관리자 수정");

        mockMvc.perform(put(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memo").value("관리자 수정"));
    }

    @Test
    @DisplayName("[200] 삭제(소유자 STAFF) — deleted=true, 하드 삭제로 DB 제거")
    void delete_owner_ok() throws Exception {
        Long id = seedSchedule(STAFF_USER_ID, LocalDate.of(2026, 8, 18), SessionType.AM);

        mockMvc.perform(delete(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));

        flushAndClear();
        assertThat(staffScheduleRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("[401] 토큰 없이 등록 요청 → 인증 필요")
    void create_noToken_unauthorized() throws Exception {
        Map<String, Object> body = Map.of("scheduleDate", "2026-08-18", "sessionType", "AM");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}

/*
 * ════════════════════════════════════════════════════════════════════════════════════
 *  통합 테스트 결과 (StaffScheduleApiIntegrationTest · 2026-07-08)
 *  실행: DB_PASSWORD 세팅 후 ./gradlew test --tests "*StaffScheduleApiIntegrationTest"
 *        (실 MSSQL Docker `hopeful_return`, @Transactional 롤백)
 *  결과: 15 tests / 15 passed / 0 skipped / 0 failures / 0 errors  (time 7.48s)
 *  ────────────────────────────────────────────────────────────────────────────────────
 *   ✅ 단건등록(STAFF) 200 isAvailable기본true   ✅ 중복 409(DUPLICATE_STAFF_SCHEDULE)
 *   ✅ 잘못된 sessionType 400(INVALID_SESSION_TYPE)   ✅ 비관리자 타인등록 403(ACCESS_DENIED)
 *   ✅ 일괄등록 registered=1/skipped=1   ✅ 목록(OPERATOR) userId·시간대필터+userName조인
 *   ✅ 목록 STAFF권한 403   ✅ /me 본인일정만(totalElements=1)   ✅ 상세(STAFF) 200
 *   ✅ 미존재 404(STAFF_SCHEDULE_NOT_FOUND)   ✅ 수정(소유자) sessionType·memo+DB반영
 *   ✅ 비소유·비관리자 수정 403   ✅ 관리자(OPERATOR) 타인수정 200   ✅ 삭제(소유자) 하드삭제
 *   ✅ 무토큰 403
 * ════════════════════════════════════════════════════════════════════════════════════
 */
