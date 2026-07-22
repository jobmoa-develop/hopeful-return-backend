package com.jobmoa.hopefulreturn.participant.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import com.jobmoa.hopefulreturn.participant.support.MatchKeyGenerator;
import com.jobmoa.hopefulreturn.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참여자 API HTTP 통합 테스트 — 실제 SecurityConfig(@PreAuthorize)·JwtAuthenticationFilter·
 * GlobalExceptionHandler·JPA(실 DB)까지 전 구간을 MockMvc로 검증한다.
 * 토큰은 실제 {@link JwtTokenProvider}로 발급(oper01=OPERATOR/userId6, admin01=ADMIN/userId1).
 * 각 테스트는 @Transactional 로 롤백되어 DB를 오염시키지 않는다.
 *
 * 결과 요약은 파일 하단 주석(=== 상세 통합 테스트 결과 ===) 참고.
 *
 * 실행 조건: 실제 MSSQL이 필요하므로 {@code DB_PASSWORD} 환경변수가 있을 때만 활성화한다.
 * → DB 없는 CI/기본 {@code ./gradlew build}에서는 자동 스킵(빌드 DB-free 방침 유지),
 *   로컬은 {@code DB_PASSWORD=... ./gradlew test} 로 실행. (CI 자동 검증은 추후 Testcontainers 도입 시)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class ParticipantApiIntegrationTest {

    private static final String BASE = "/api/participants";
    private static final Long COUNSELOR_USER_ID = 7L; // counsel01(상담사1)

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ParticipantRepository participantRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseParticipantRepository courseParticipantRepository;
    @Autowired
    private CourseParticipantCounselorRepository courseParticipantCounselorRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private String opToken;
    private String adminToken;
    private String headToken;

    @BeforeEach
    void setUp() {
        opToken = jwtTokenProvider.createAccessToken(6L, "oper01", List.of("OPERATOR"));
        adminToken = jwtTokenProvider.createAccessToken(1L, "admin01", List.of("ADMIN"));
        headToken = jwtTokenProvider.createAccessToken(2L, "head01", List.of("HEAD_OFFICE"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Long seed(String name, Integer birthYear, String phone) {
        ParticipantEntity e = ParticipantEntity.builder()
                .name(name)
                .birthYear(birthYear)
                .phone(phone)
                .matchKey(MatchKeyGenerator.generate(name, birthYear, phone))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return participantRepository.saveAndFlush(e).getParticipantId();
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return courseRepository.saveAndFlush(course).getCourseId();
    }

    // ✅ PASS
    @Test
    @DisplayName("[201/200] 등록(HEAD_OFFICE) → success=true, participantId 반환, matchKey DB 저장")
    void create_ok() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", "김철수", "birthYear", 1978, "phone", "010-5678-1234"));

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(headToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.participantId").isNumber());

        // matchKey = 이니셜(로마자)_생년_전화뒤4
        ParticipantEntity saved = participantRepository.findFirstByPhoneOrderByParticipantIdAsc("010-5678-1234")
                .orElseThrow();
        assertThat(saved.getMatchKey()).isEqualTo("KCS_1978_1234");
    }

    @Test
    @DisplayName("[200] 통합 등록(HEAD_OFFICE) — 참여자 + 수강(CONFIRMED) + 상담사 배정이 한 번에 생성된다")
    void create_withEnrollment_ok() throws Exception {
        Long courseId = seedCourse();
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "김철수", "birthYear", 1978, "phone", "010-5678-1234",
                "enrollment", Map.of(
                        "courseId", courseId,
                        "inflowType", "워크넷",
                        "applyDate", "2026-08-01",
                        "receptionDate", "2026-08-02",
                        "basicEducation", "Y",
                        "counselors", List.of(
                                Map.of("counselorId", COUNSELOR_USER_ID, "status", "PRE_SESSION")))));

        String response = mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(headToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participantId").isNumber())
                .andExpect(jsonPath("$.data.matchKey").value("KCS_1978_1234"))
                .andExpect(jsonPath("$.data.courseParticipantId").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long courseParticipantId = objectMapper.readTree(response)
                .path("data").path("courseParticipantId").asLong();
        CourseParticipantEntity cp = courseParticipantRepository.findById(courseParticipantId).orElseThrow();
        assertThat(cp.getStatus()).isEqualTo(CourseParticipantStatus.CONFIRMED);
        List<CourseParticipantCounselorEntity> counselorRows =
                courseParticipantCounselorRepository.findByCourseParticipantId(courseParticipantId);
        assertThat(counselorRows).hasSize(1);
        assertThat(counselorRows.get(0).getCounselorId()).isEqualTo(COUNSELOR_USER_ID);
        assertThat(counselorRows.get(0).getStatus()).isEqualTo(CounselingType.PRE_SESSION);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("[404] 통합 등록 시 존재하지 않는 courseId — 참여자 생성도 롤백된다")
    void create_withEnrollment_invalidCourse_rollsBack() throws Exception {
        String phone = "010-7777-8888";
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "김철수", "birthYear", 1978, "phone", phone,
                "enrollment", Map.of("courseId", 99999999L)));

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(headToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("강좌를 찾을 수 없습니다."));

        // 트랜잭션 미소유 테스트 — 서비스 트랜잭션이 실제 롤백되어 참여자 행이 남지 않아야 한다
        assertThat(participantRepository.findByPhone(phone)).isEmpty();
    }

    @Test
    @DisplayName("[200] 목록 조회 — 최신 수강건 요약(latestEnrollment)이 포함된다")
    void list_withLatestEnrollment() throws Exception {
        Long participantId = seed("김철수", 1978, "010-5678-1234");
        Long courseId = seedCourse();
        CourseParticipantEntity cp = CourseParticipantEntity.builder()
                .courseId(courseId)
                .participantId(participantId)
                .status(CourseParticipantStatus.CONFIRMED)
                .contactAttempt(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Long cpId = courseParticipantRepository.saveAndFlush(cp).getCourseParticipantId();
        courseParticipantCounselorRepository.saveAndFlush(CourseParticipantCounselorEntity.builder()
                .courseParticipantId(cpId)
                .counselorId(COUNSELOR_USER_ID)
                .status(CounselingType.PRE_SESSION)
                .counselingStartedAt(LocalDateTime.of(2026, 8, 5, 14, 0))
                .counselingEndedAt(LocalDateTime.of(2026, 8, 5, 15, 0))
                .createdAt(LocalDateTime.now())
                .build());
        flushAndClear();

        mockMvc.perform(get(BASE).param("phone", "010-5678-1234")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].matchKey").value("KCS_1978_1234"))
                .andExpect(jsonPath("$.data.content[0].latestEnrollment.courseParticipantId").value(cpId))
                .andExpect(jsonPath("$.data.content[0].latestEnrollment.regionName").value("서울"))
                .andExpect(jsonPath("$.data.content[0].latestEnrollment.localCourseNumber").value(1))
                .andExpect(jsonPath("$.data.content[0].latestEnrollment.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.content[0].latestEnrollment.preCounselingCompleted").value(true))
                .andExpect(jsonPath("$.data.content[0].latestEnrollment.counselors[0].completed").value(true));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 목록 조회(OPERATOR) — 페이지 응답 봉투")
    void list_ok() throws Exception {
        seed("김철수", 1978, "010-5678-1234");

        mockMvc.perform(get(BASE).param("page", "0").param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 전화번호 중복확인 — 존재 시 duplicate=true + participantId")
    void checkPhone_duplicate() throws Exception {
        Long id = seed("김철수", 1978, "010-5678-1234");

        mockMvc.perform(get(BASE + "/check-phone").param("phone", "010-5678-1234")
                        .header(HttpHeaders.AUTHORIZATION, bearer(headToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(true))
                .andExpect(jsonPath("$.data.participantId").value(id));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 전화번호 중복확인 — 미존재 시 duplicate=false")
    void checkPhone_notDuplicate() throws Exception {
        mockMvc.perform(get(BASE + "/check-phone").param("phone", "010-0000-0000")
                        .header(HttpHeaders.AUTHORIZATION, bearer(headToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(false));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 상세 조회(인증 사용자) — 필드 매핑")
    void detail_ok() throws Exception {
        Long id = seed("김철수", 1978, "010-5678-1234");

        mockMvc.perform(get(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participantId").value(id))
                .andExpect(jsonPath("$.data.name").value("김철수"))
                .andExpect(jsonPath("$.data.phone").value("010-5678-1234"));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 수정(OPERATOR) — updated=true, DB 반영 + matchKey 재계산")
    void update_ok() throws Exception {
        Long id = seed("김철수", 1978, "010-5678-1234");
        String body = objectMapper.writeValueAsString(
                Map.of("name", "김영희", "birthYear", 1979, "phone", "010-1111-2222"));

        mockMvc.perform(put(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(true));

        flushAndClear();
        ParticipantEntity updated = participantRepository.findById(id).orElseThrow();
        assertThat(updated.getName()).isEqualTo("김영희");
        assertThat(updated.getMatchKey()).isEqualTo("KYH_1979_2222");
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 삭제(ADMIN) — deleted=true, 하드 삭제로 DB에서 제거")
    void delete_ok() throws Exception {
        Long id = seed("김철수", 1978, "010-5678-1234");

        mockMvc.perform(delete(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));

        flushAndClear();
        assertThat(participantRepository.findById(id)).isEmpty();
    }

    // ✅ PASS
    @Test
    @DisplayName("[401] 토큰 없이 등록 요청 → 인증 필요")
    void create_noToken_unauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", "홍길동", "phone", "010-0000-0000"));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    // ✅ PASS
    @Test
    @DisplayName("[400] 등록 시 name 공백 → 입력 검증 실패")
    void create_blankName_badRequest() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", "", "birthYear", 1980, "phone", "010-2222-3333"));

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(headToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ✅ PASS
    @Test
    @DisplayName("[404] 존재하지 않는 참여자 상세 조회 → PARTICIPANT_NOT_FOUND")
    void detail_notFound() throws Exception {
        mockMvc.perform(get(BASE + "/99999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("참여자를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("[403] 등록(OPERATOR) — 등록은 관리 롤 전용(#51)이라 접근 차단")
    void create_operatorRole_forbidden() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", "김철수", "birthYear", 1978, "phone", "010-5678-1234"));

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[403] 전화번호 중복확인(OPERATOR) — 등록 플로우 전용 권한이라 접근 차단")
    void checkPhone_operatorRole_forbidden() throws Exception {
        mockMvc.perform(get(BASE + "/check-phone").param("phone", "010-5678-1234")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[200] 목록 조회(ADMIN) — 8롤 개방 회귀 방지(#51)")
    void list_admin_ok() throws Exception {
        seed("김철수", 1978, "010-5678-1234");

        mockMvc.perform(get(BASE).param("page", "0").param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // #15 해결(GlobalExceptionHandler에 AuthorizationDeniedException → 403 핸들러 추가, 2026-07-06 종료)로 재활성화.
    @Test
    @DisplayName("[403] 삭제(OPERATOR=권한부족) — ACCESS_DENIED")
    void delete_wrongRole_shouldBeForbidden() throws Exception {
        Long id = seed("김철수", 1978, "010-5678-1234");

        mockMvc.perform(delete(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isForbidden());
    }

    /**
     * 대기 중인 변경을 DB에 반영하고 1차 캐시를 비워, 이후 조회에서 지연 연관
     * (course·region 등 insertable=false 연관)이 정상 초기화되도록 한다. (트랜잭션은 종료 시 롤백)
     * 실 HTTP(Postman)는 매 요청이 새 트랜잭션이라 이 문제가 없다.
     */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}

/*
 * ════════════════════════════════════════════════════════════════════════════════════
 *  상세 통합 테스트 결과 (ParticipantApiIntegrationTest · 2026-07-06)
 *  실행: ./gradlew test --tests "*ParticipantApiIntegrationTest"   (실 DB, @Transactional 롤백)
 *  결과: 11 tests / 10 passed / 1 skipped(@Disabled #15) / 0 failures / 0 errors
 *  ────────────────────────────────────────────────────────────────────────────────────
 *   ✅ 등록(OPERATOR) 200 + matchKey=KCS_1978_1234 DB 저장 확인
 *   ✅ 목록 200(페이지 봉투)   ✅ 중복확인 true/false   ✅ 상세 200(필드 매핑)
 *   ✅ 수정 200 + matchKey 재계산(KYH_1979_2222)        ✅ 삭제(ADMIN) 200 하드삭제
 *   ✅ 무토큰 403             ✅ name 공백 400           ✅ 미존재 404(PARTICIPANT_NOT_FOUND)
 *   ⛔ 권한부족 삭제(OPERATOR) → @Disabled: 현재 500(공통결함 #15), 수정 후 403 기대
 * ════════════════════════════════════════════════════════════════════════════════════
 */
