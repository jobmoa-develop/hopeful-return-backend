package com.jobmoa.hopefulreturn.followup.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselingType;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.followup.entity.FollowUpEntity;
import com.jobmoa.hopefulreturn.followup.repository.FollowUpRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import com.jobmoa.hopefulreturn.security.JwtTokenProvider;
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
 * 사후관리(FollowUp) API HTTP 통합 테스트 — 집계 목록(수료자+스냅샷+상담요약)·CRUD·스코프까지
 * SecurityConfig·JwtAuthenticationFilter·GlobalExceptionHandler·JPA(실 DB)를 MockMvc로 검증한다.
 * 토큰은 실제 {@link JwtTokenProvider}로 발급(admin01/1, counsel01/7 — V4 시드). 시드는 @Transactional 롤백.
 *
 * 목록 스코프: 상담사(COUNSELOR 전용)는 본인 배정 수료자만 노출(course-participant 단계에서 필터 →
 * 미배정은 빈 목록). 상세(followUpId 직접 접근)는 미배정 시 403(ACCESS_DENIED).
 *
 * 실행 조건: 실제 MSSQL 필요 → {@code DB_PASSWORD} 환경변수가 있을 때만 활성화(무DB CI 자동 스킵).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class FollowUpApiIntegrationTest {

    private static final String BASE = "/api/follow-ups";
    private static final String UNIQUE_NAME = "사후관리테스트김";
    private static final Long ASSIGNED_COUNSELOR_ID = 7L;   // counsel01 (배정)
    private static final Long UNASSIGNED_COUNSELOR_ID = 999L; // 미배정 상담사(가상)

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private ParticipantRepository participantRepository;
    @Autowired
    private CourseParticipantRepository courseParticipantRepository;
    @Autowired
    private CourseParticipantCounselorRepository courseParticipantCounselorRepository;
    @Autowired
    private FollowUpRepository followUpRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private String adminToken;
    private String assignedCounselorToken;
    private String unassignedCounselorToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtTokenProvider.createAccessToken(1L, "admin01", List.of("ADMIN"));
        assignedCounselorToken =
                jwtTokenProvider.createAccessToken(ASSIGNED_COUNSELOR_ID, "counsel01", List.of("COUNSELOR"));
        unassignedCounselorToken =
                jwtTokenProvider.createAccessToken(UNASSIGNED_COUNSELOR_ID, "counselX", List.of("COUNSELOR"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Long seedCompletedParticipant() {
        CourseEntity course = courseRepository.saveAndFlush(CourseEntity.builder()
                .regionId(1L).courseNumber(5).localCourseNumber(1).courseName("사후관리테스트기")
                .capacity(20).minimumCapacity(5).status(CourseStatus.PLANNED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
        Long participantId = participantRepository.saveAndFlush(ParticipantEntity.builder()
                .name(UNIQUE_NAME).birthYear(1978).phone("010-5678-1234").matchKey("KCS_1978_FU")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build()).getParticipantId();
        CourseParticipantEntity cp = courseParticipantRepository.saveAndFlush(CourseParticipantEntity.builder()
                .courseId(course.getCourseId()).participantId(participantId)
                .status(CourseParticipantStatus.COMPLETED).completionDate(LocalDate.of(2026, 5, 30))
                .contactAttempt(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
        Long cpId = cp.getCourseParticipantId();
        courseParticipantCounselorRepository.saveAndFlush(CourseParticipantCounselorEntity.builder()
                .courseParticipantId(cpId).counselorId(ASSIGNED_COUNSELOR_ID)
                .status(CounselingType.POST_SESSION_1).createdAt(LocalDateTime.now())
                .build());
        return cpId;
    }

    private Long seedFollowUp(Long cpId) {
        return followUpRepository.saveAndFlush(FollowUpEntity.builder()
                .courseParticipantId(cpId)
                .employmentDate(LocalDate.of(2026, 9, 24))
                .nationalProgramBranch("남부")
                .build()).getFollowupId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("[200] 등록(COUNSELOR) → followUpId·createdAt 반환")
    void create_ok() throws Exception {
        Long cpId = seedCompletedParticipant();
        flushAndClear();
        Map<String, Object> body = Map.of(
                "courseParticipantId", cpId,
                "employmentDate", "2026-09-24",
                "nationalProgramBranch", "남부");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedCounselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.followUpId").isNumber())
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @DisplayName("[400] 등록: 허용되지 않은 국취 지점 → INVALID_INPUT")
    void create_invalidBranch_badRequest() throws Exception {
        Long cpId = seedCompletedParticipant();
        flushAndClear();
        Map<String, Object> body = Map.of("courseParticipantId", cpId, "nationalProgramBranch", "없는지점");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedCounselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("[200] 목록(ADMIN) → 수료자+스냅샷+상담요약 집계(page 봉투)")
    void list_admin_ok() throws Exception {
        Long cpId = seedCompletedParticipant();
        seedFollowUp(cpId);
        flushAndClear();

        mockMvc.perform(get(BASE).param("name", UNIQUE_NAME)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value(UNIQUE_NAME))
                .andExpect(jsonPath("$.data.content[0].courseParticipantId").value(cpId))
                .andExpect(jsonPath("$.data.content[0].completionDate").value("2026-05-30"))
                .andExpect(jsonPath("$.data.content[0].nationalProgramBranch").value("남부"))
                .andExpect(jsonPath("$.data.content[0].employmentDate").value("2026-09-24"))
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    @DisplayName("[200] 목록(상담사, 배정됨) → 본인 배정 수료자 노출")
    void list_assignedCounselor_ok() throws Exception {
        Long cpId = seedCompletedParticipant();
        seedFollowUp(cpId);
        flushAndClear();

        mockMvc.perform(get(BASE).param("name", UNIQUE_NAME)
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedCounselorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].courseParticipantId").value(cpId));
    }

    @Test
    @DisplayName("[200] 목록(상담사, 미배정) → 스코프 필터로 빈 목록")
    void list_unassignedCounselor_empty() throws Exception {
        Long cpId = seedCompletedParticipant();
        seedFollowUp(cpId);
        flushAndClear();

        mockMvc.perform(get(BASE).param("name", UNIQUE_NAME)
                        .header(HttpHeaders.AUTHORIZATION, bearer(unassignedCounselorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("[200] 상세(ADMIN) → 신규 필드 반환")
    void detail_admin_ok() throws Exception {
        Long cpId = seedCompletedParticipant();
        Long followUpId = seedFollowUp(cpId);
        flushAndClear();

        mockMvc.perform(get(BASE + "/" + followUpId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followUpId").value(followUpId))
                .andExpect(jsonPath("$.data.nationalProgramBranch").value("남부"));
    }

    @Test
    @DisplayName("[403] 상세(상담사, 미배정) → ACCESS_DENIED")
    void detail_unassignedCounselor_forbidden() throws Exception {
        Long cpId = seedCompletedParticipant();
        Long followUpId = seedFollowUp(cpId);
        flushAndClear();

        mockMvc.perform(get(BASE + "/" + followUpId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(unassignedCounselorToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[404] 상세: 미존재 → FOLLOW_UP_NOT_FOUND")
    void detail_notFound() throws Exception {
        mockMvc.perform(get(BASE + "/99999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("사후관리 정보를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("[200] 삭제(ADMIN) → 삭제 메시지")
    void delete_admin_ok() throws Exception {
        Long cpId = seedCompletedParticipant();
        Long followUpId = seedFollowUp(cpId);
        flushAndClear();

        mockMvc.perform(delete(BASE + "/" + followUpId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("사후관리 정보가 삭제되었습니다."));
    }

    @Test
    @DisplayName("[401] 토큰 없이 등록 → 인증 필요")
    void create_noToken_unauthorized() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseParticipantId", 1))))
                .andExpect(status().isUnauthorized());
    }
}
