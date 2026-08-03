package com.jobmoa.hopefulreturn.followupcounsel.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.jobmoa.hopefulreturn.followupcounsel.entity.FollowUpCounselEntity;
import com.jobmoa.hopefulreturn.followupcounsel.repository.FollowUpCounselRepository;
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
 * 사후관리 상담(FollowUpCounsel) API HTTP 통합 테스트 — 전 구간(Security·JWT·JPA)을 MockMvc로 검증.
 * 토큰은 실제 {@link JwtTokenProvider}로 발급(admin01/1, counsel01/7 — V4 시드). 시드는 @Transactional 롤백.
 *
 * 실행 조건: 실제 MSSQL 필요 → {@code DB_PASSWORD} 환경변수가 있을 때만 활성화(무DB CI 자동 스킵).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class FollowUpCounselApiIntegrationTest {

    private static final String BASE = "/api/follow-up-counsels";
    private static final Long ASSIGNED_COUNSELOR_ID = 7L;
    private static final Long UNASSIGNED_COUNSELOR_ID = 999L;

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
    private FollowUpCounselRepository followUpCounselRepository;
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
                .regionId(1L).courseNumber(5).localCourseNumber(1).courseName("상담테스트기")
                .capacity(20).minimumCapacity(5).status(CourseStatus.PLANNED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
        Long participantId = participantRepository.saveAndFlush(ParticipantEntity.builder()
                .name("이영희").birthYear(1980).phone("010-1111-2222").matchKey("LYH_1980_FC")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build()).getParticipantId();
        CourseParticipantEntity cp = courseParticipantRepository.saveAndFlush(CourseParticipantEntity.builder()
                .courseId(course.getCourseId()).participantId(participantId)
                .status(CourseParticipantStatus.COMPLETED).contactAttempt(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
        Long cpId = cp.getCourseParticipantId();
        courseParticipantCounselorRepository.saveAndFlush(CourseParticipantCounselorEntity.builder()
                .courseParticipantId(cpId).counselorId(ASSIGNED_COUNSELOR_ID)
                .status(CounselingType.POST_SESSION_1).createdAt(LocalDateTime.now())
                .build());
        return cpId;
    }

    private Long seedCounsel(Long cpId) {
        LocalDateTime now = LocalDateTime.now();
        return followUpCounselRepository.saveAndFlush(FollowUpCounselEntity.builder()
                .courseParticipantId(cpId).counselNumber(1)
                .counselDate(LocalDate.of(2026, 9, 24)).counselStatus("landline")
                .counselMemo("현재 구직 중").createdAt(now).updatedAt(now)
                .build()).getFollowUpCounselId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("[200] 등록(COUNSELOR) → followUpCounselId·createdAt 반환")
    void create_ok() throws Exception {
        Long cpId = seedCompletedParticipant();
        flushAndClear();
        Map<String, Object> body = Map.of(
                "courseParticipantId", cpId, "counselNumber", 1,
                "counselDate", "2026-09-24", "counselStatus", "landline", "counselMemo", "현재 구직 중");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedCounselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followUpCounselId").isNumber())
                .andExpect(jsonPath("$.data.counselNumber").value(1))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @DisplayName("[400] 등록: 허용되지 않은 counselStatus → INVALID_INPUT")
    void create_invalidStatus_badRequest() throws Exception {
        Long cpId = seedCompletedParticipant();
        flushAndClear();
        Map<String, Object> body = Map.of(
                "courseParticipantId", cpId, "counselStatus", "email");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedCounselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[200] 목록(ADMIN) → 조회, 필드 반환")
    void list_admin_ok() throws Exception {
        Long cpId = seedCompletedParticipant();
        seedCounsel(cpId);
        flushAndClear();

        mockMvc.perform(get(BASE).param("courseParticipantId", String.valueOf(cpId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].counselStatus").value("landline"))
                .andExpect(jsonPath("$.data.content[0].counselNumber").value(1));
    }

    @Test
    @DisplayName("[403] 목록(상담사, 미배정) → ACCESS_DENIED")
    void list_unassignedCounselor_forbidden() throws Exception {
        Long cpId = seedCompletedParticipant();
        seedCounsel(cpId);
        flushAndClear();

        mockMvc.perform(get(BASE).param("courseParticipantId", String.valueOf(cpId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(unassignedCounselorToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("[200] 상세(상담사, 배정됨) → 조회 가능")
    void detail_assignedCounselor_ok() throws Exception {
        Long cpId = seedCompletedParticipant();
        Long id = seedCounsel(cpId);
        flushAndClear();

        mockMvc.perform(get(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedCounselorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followUpCounselId").value(id))
                .andExpect(jsonPath("$.data.counselStatus").value("landline"));
    }

    @Test
    @DisplayName("[404] 상세: 미존재 → FOLLOW_UP_COUNSEL_NOT_FOUND")
    void detail_notFound() throws Exception {
        mockMvc.perform(get(BASE + "/99999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("사후관리 상담 정보를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("[200] 수정(COUNSELOR) → updatedAt 반환, 반영")
    void update_ok() throws Exception {
        Long cpId = seedCompletedParticipant();
        Long id = seedCounsel(cpId);
        flushAndClear();
        Map<String, Object> body = Map.of(
                "counselDate", "2026-09-25", "counselStatus", "text", "counselMemo", "문자 발송 완료");

        mockMvc.perform(put(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedCounselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followUpCounselId").value(id))
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    @DisplayName("[200] 삭제(ADMIN) → 삭제 메시지")
    void delete_admin_ok() throws Exception {
        Long cpId = seedCompletedParticipant();
        Long id = seedCounsel(cpId);
        flushAndClear();

        mockMvc.perform(delete(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("사후관리 상담 정보가 삭제되었습니다."));
    }

    @Test
    @DisplayName("[401] 토큰 없이 등록 → 인증 필요")
    void create_noToken_unauthorized() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseParticipantId", 1))))
                .andExpect(status().isUnauthorized());
    }
}
