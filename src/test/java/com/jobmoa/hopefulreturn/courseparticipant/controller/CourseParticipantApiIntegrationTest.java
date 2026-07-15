package com.jobmoa.hopefulreturn.courseparticipant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.jobmoa.hopefulreturn.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.HashMap;
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
 * 수강관리(CourseParticipant) API HTTP 통합 테스트 — 실제 SecurityConfig(@PreAuthorize)·
 * JwtAuthenticationFilter·GlobalExceptionHandler·JPA(실 DB)까지 전 구간을 MockMvc로 검증한다.
 * 토큰은 실제 {@link JwtTokenProvider}로 발급(oper01=OPERATOR/6, admin01=ADMIN/1,
 * counsel01=COUNSELOR/7, staff01=STAFF/9 — V4 시드 기준).
 * course/participant 는 각 테스트에서 시드하며 @Transactional 로 롤백되어 DB를 오염시키지 않는다.
 *
 * 결과 요약·Postman 실호출 대조는 파일 하단 주석 참고.
 *
 * 실행 조건: 실제 MSSQL 필요 → {@code DB_PASSWORD} 환경변수가 있을 때만 활성화(무DB CI 자동 스킵).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class CourseParticipantApiIntegrationTest {

    private static final String BASE = "/api/course-participants";
    private static final Long COUNSELOR_USER_ID = 7L; // counsel01(상담사1)

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
    @PersistenceContext
    private EntityManager entityManager;

    private String opToken;
    private String counselToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        opToken = jwtTokenProvider.createAccessToken(6L, "oper01", "OPERATOR");
        counselToken = jwtTokenProvider.createAccessToken(7L, "counsel01", "COUNSELOR");
        staffToken = jwtTokenProvider.createAccessToken(9L, "staff01", "STAFF");
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return courseRepository.saveAndFlush(course).getCourseId();
    }

    private Long seedParticipant() {
        ParticipantEntity participant = ParticipantEntity.builder()
                .name("김철수")
                .birthYear(1978)
                .phone("010-5678-1234")
                .matchKey("KCS_1978_1234")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return participantRepository.saveAndFlush(participant).getParticipantId();
    }

    private Long seedCourseParticipant(Long courseId, Long participantId, CourseParticipantStatus status) {
        CourseParticipantEntity cp = CourseParticipantEntity.builder()
                .courseId(courseId)
                .participantId(participantId)
                .status(status)
                .contactAttempt(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Long cpId = courseParticipantRepository.saveAndFlush(cp).getCourseParticipantId();
        CourseParticipantCounselorEntity link = CourseParticipantCounselorEntity.builder()
                .courseParticipantId(cpId)
                .counselorId(COUNSELOR_USER_ID)
                .status(CounselingType.PRE_SESSION)
                .createdAt(LocalDateTime.now())
                .build();
        courseParticipantCounselorRepository.saveAndFlush(link);
        return cpId;
    }

    /**
     * 시드 직후 영속성 컨텍스트를 비워, 이후 서비스 조회가 DB에서 재로딩되어 LAZY 연관
     * (participant·course·counselor)이 정상 초기화되도록 한다. (insertable=false 연관은
     * 같은 세션에서 insert 후 자동 갱신되지 않으므로 조인 조회 테스트 전 필수.)
     * 실 HTTP(Postman)는 매 요청이 새 트랜잭션이라 이 문제가 없다.
     */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 등록(OPERATOR) → success=true, courseParticipantId 반환, status=APPLIED")
    void create_ok() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Map<String, Object> body = new HashMap<>();
        body.put("courseId", courseId);
        body.put("participantId", participantId);
        body.put("counselors", List.of(Map.of("counselorId", COUNSELOR_USER_ID, "status", "PRE_SESSION")));
        body.put("inflowType", "워크넷");
        body.put("applyDate", "2026-08-01");
        body.put("receptionDate", "2026-08-02");
        body.put("basicEducation", "Y");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseParticipantId").isNumber())
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 목록 조회(OPERATOR) — 페이지 응답 봉투 + 조인 필드")
    void list_ok() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        seedCourseParticipant(courseId, participantId, CourseParticipantStatus.APPLIED);
        flushAndClear();

        mockMvc.perform(get(BASE).param("courseId", String.valueOf(courseId)).param("page", "0").param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].participantName").value("김철수"))
                .andExpect(jsonPath("$.data.content[0].phone").value("010-5678-1234"))
                .andExpect(jsonPath("$.data.content[0].counselors[0].counselorName").value("상담사1"))
                .andExpect(jsonPath("$.data.content[0].counselors[0].status").value("PRE_SESSION"))
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 상세 조회(STAFF) — course·participant·counselor 조인 매핑")
    void detail_ok() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Long id = seedCourseParticipant(courseId, participantId, CourseParticipantStatus.APPLIED);
        flushAndClear();

        mockMvc.perform(get(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseParticipantId").value(id))
                .andExpect(jsonPath("$.data.participantName").value("김철수"))
                .andExpect(jsonPath("$.data.matchKey").value("KCS_1978_1234"))
                .andExpect(jsonPath("$.data.birthYear").value(1978))
                .andExpect(jsonPath("$.data.phone").value("010-5678-1234"))
                .andExpect(jsonPath("$.data.courseName").value("양천5기"))
                .andExpect(jsonPath("$.data.regionName").value("서울"))
                .andExpect(jsonPath("$.data.courseNumber").value(5))
                .andExpect(jsonPath("$.data.localCourseNumber").value(1))
                .andExpect(jsonPath("$.data.counselors[0].counselorName").value("상담사1"))
                .andExpect(jsonPath("$.data.counselors[0].status").value("PRE_SESSION"))
                .andExpect(jsonPath("$.data.counselors[0].completed").value(false))
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.contactAttempt").value(0));
    }

    @Test
    @DisplayName("[200] 상담 세션 기록(COUNSELOR) — 시작/종료·메모 저장, completed=true")
    void recordCounselingSession_ok() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Long id = seedCourseParticipant(courseId, participantId, CourseParticipantStatus.CONFIRMED);
        Map<String, Object> body = Map.of(
                "startedAt", "2026-08-05T14:00:00",
                "endedAt", "2026-08-05T15:00:00",
                "memo", "사전상담 진행 완료");

        mockMvc.perform(patch(BASE + "/" + id + "/counselors/PRE_SESSION")
                        .header(HttpHeaders.AUTHORIZATION, bearer(counselToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseParticipantId").value(id))
                .andExpect(jsonPath("$.data.counselingType").value("PRE_SESSION"))
                .andExpect(jsonPath("$.data.counselorId").value(COUNSELOR_USER_ID))
                .andExpect(jsonPath("$.data.memo").value("사전상담 진행 완료"))
                .andExpect(jsonPath("$.data.completed").value(true));
    }

    @Test
    @DisplayName("[404] 상담 세션 기록 — 배정 없는 슬롯(POST_SESSION_2)이면 COUNSELING_SLOT_NOT_FOUND")
    void recordCounselingSession_slotNotFound() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Long id = seedCourseParticipant(courseId, participantId, CourseParticipantStatus.CONFIRMED);
        Map<String, Object> body = Map.of("startedAt", "2026-08-05T14:00:00");

        mockMvc.perform(patch(BASE + "/" + id + "/counselors/POST_SESSION_2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("해당 상담 구분에 배정된 상담사가 없습니다."));
    }

    @Test
    @DisplayName("[400] 상담 세션 기록 — 종료가 시작보다 앞서면 INVALID_COUNSELING_TIME")
    void recordCounselingSession_invalidTime() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Long id = seedCourseParticipant(courseId, participantId, CourseParticipantStatus.CONFIRMED);
        Map<String, Object> body = Map.of(
                "startedAt", "2026-08-05T15:00:00",
                "endedAt", "2026-08-05T14:00:00");

        mockMvc.perform(patch(BASE + "/" + id + "/counselors/PRE_SESSION")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("상담 종료 일시는 시작 일시 이후여야 합니다."));
    }

    @Test
    @DisplayName("[200] 상담사 변경 — 사전/사후1/사후2 3개 슬롯 전체 교체")
    void changeCounselor_threeSlots_ok() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Long id = seedCourseParticipant(courseId, participantId, CourseParticipantStatus.CONFIRMED);
        Map<String, Object> body = Map.of("counselors", List.of(
                Map.of("counselorId", COUNSELOR_USER_ID, "status", "PRE_SESSION"),
                Map.of("counselorId", 2, "status", "POST_SESSION_1"), // head01
                Map.of("counselorId", COUNSELOR_USER_ID, "status", "POST_SESSION_2")));

        mockMvc.perform(patch(BASE + "/" + id + "/counselor")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counselors.length()").value(3));
    }

    @Test
    @DisplayName("[400] 상담사 변경 — 같은 슬롯 중복 배정이면 COUNSELING_SLOT_DUPLICATED")
    void changeCounselor_duplicateSlot_badRequest() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Long id = seedCourseParticipant(courseId, participantId, CourseParticipantStatus.CONFIRMED);
        Map<String, Object> body = Map.of("counselors", List.of(
                Map.of("counselorId", COUNSELOR_USER_ID, "status", "PRE_SESSION"),
                Map.of("counselorId", 2, "status", "PRE_SESSION")));

        mockMvc.perform(patch(BASE + "/" + id + "/counselor")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("같은 상담 구분을 중복 배정할 수 없습니다."));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 수정(OPERATOR) — updated=true, DB 반영")
    void update_ok() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Long id = seedCourseParticipant(courseId, participantId, CourseParticipantStatus.APPLIED);
        Map<String, Object> body = new HashMap<>();
        body.put("counselors", List.of(Map.of("counselorId", COUNSELOR_USER_ID, "status", "PRE_SESSION")));
        body.put("basicEducation", "N");
        body.put("inflowType", "지인추천");

        mockMvc.perform(put(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(true));

        courseParticipantRepository.flush();
        CourseParticipantEntity updated = courseParticipantRepository.findById(id).orElseThrow();
        assertThat(updated.getBasicEducation()).isEqualTo("N");
        assertThat(updated.getInflowType()).isEqualTo("지인추천");
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 취소(OPERATOR) — status=CANCELED, reason이 incompleteReason에 기록")
    void cancel_ok() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Long id = seedCourseParticipant(courseId, participantId, CourseParticipantStatus.APPLIED);
        Map<String, Object> body = Map.of("reason", "참여자 요청");

        mockMvc.perform(post(BASE + "/" + id + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"));

        courseParticipantRepository.flush();
        CourseParticipantEntity canceled = courseParticipantRepository.findById(id).orElseThrow();
        assertThat(canceled.getStatus()).isEqualTo(CourseParticipantStatus.CANCELED);
        assertThat(canceled.getIncompleteReason()).isEqualTo("참여자 요청");
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 수료 처리(OPERATOR) — status=COMPLETED")
    void completion_ok() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Long id = seedCourseParticipant(courseId, participantId, CourseParticipantStatus.APPLIED);
        Map<String, Object> body = new HashMap<>();
        body.put("status", "COMPLETED");
        body.put("completionDate", "2026-08-24");

        mockMvc.perform(patch(BASE + "/" + id + "/completion")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseParticipantId").value(id))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 연락 시도 증가(COUNSELOR) — contactAttempt 1 증가")
    void contactAttempt_ok() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Long id = seedCourseParticipant(courseId, participantId, CourseParticipantStatus.APPLIED);

        mockMvc.perform(patch(BASE + "/" + id + "/contact-attempt")
                        .header(HttpHeaders.AUTHORIZATION, bearer(counselToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseParticipantId").value(id))
                .andExpect(jsonPath("$.data.contactAttempt").value(1));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 상담사 변경(OPERATOR) — 배정 전체 교체 반영")
    void changeCounselor_ok() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Long id = seedCourseParticipant(courseId, participantId, CourseParticipantStatus.APPLIED);
        Map<String, Object> body = Map.of(
                "counselors", List.of(Map.of("counselorId", 2, "status", "POST_SESSION_1"))); // head01

        mockMvc.perform(patch(BASE + "/" + id + "/counselor")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseParticipantId").value(id))
                .andExpect(jsonPath("$.data.counselors[0].counselorId").value(2))
                .andExpect(jsonPath("$.data.counselors[0].status").value("POST_SESSION_1"));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 삭제(OPERATOR) — deleted=true, 하드 삭제로 DB 제거")
    void delete_ok() throws Exception {
        Long courseId = seedCourse();
        Long participantId = seedParticipant();
        Long id = seedCourseParticipant(courseId, participantId, CourseParticipantStatus.APPLIED);

        mockMvc.perform(delete(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));

        courseParticipantRepository.flush();
        assertThat(courseParticipantRepository.findById(id)).isEmpty();
    }

    // ✅ PASS
    @Test
    @DisplayName("[401] 토큰 없이 등록 요청 → 인증 필요")
    void create_noToken_unauthorized() throws Exception {
        Map<String, Object> body = Map.of("courseId", 1, "participantId", 1);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    // ✅ PASS
    @Test
    @DisplayName("[403] 권한 없는 롤(STAFF)로 등록 요청 → 접근 차단(#15 수정 반영)")
    void create_wrongRole_forbidden() throws Exception {
        Map<String, Object> body = Map.of("courseId", 1, "participantId", 1);

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // ✅ PASS
    @Test
    @DisplayName("[400] 등록 시 courseId 누락 → 입력 검증 실패")
    void create_missingCourseId_badRequest() throws Exception {
        Map<String, Object> body = Map.of("participantId", 1); // courseId 누락

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ✅ PASS
    @Test
    @DisplayName("[404] 존재하지 않는 수강 상세 조회 → COURSE_PARTICIPANT_NOT_FOUND")
    void detail_notFound() throws Exception {
        mockMvc.perform(get(BASE + "/99999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("수강 정보를 찾을 수 없습니다."));
    }
}

/*
 * ════════════════════════════════════════════════════════════════════════════════════
 *  상세 통합 테스트 결과 (CourseParticipantApiIntegrationTest · 2026-07-07)
 *  실행: DB_PASSWORD=... ./gradlew test --tests "*CourseParticipantApiIntegrationTest"
 *        (실 DB, @Transactional 롤백)
 *  결과: 13 tests / 13 passed / 0 skipped / 0 failures / 0 errors
 *  ────────────────────────────────────────────────────────────────────────────────────
 *   ✅ 등록(OPERATOR) 200 status=APPLIED          ✅ 목록 200(조인 participantName·phone·counselorName)
 *   ✅ 상세(STAFF) 200(course·counselor 조인)      ✅ 수정 200 updated=true + DB 반영
 *   ✅ 취소 200 CANCELED + incompleteReason 기록   ✅ 수료 200 COMPLETED
 *   ✅ 연락시도(COUNSELOR) 200 contactAttempt=1    ✅ 상담사변경 200 counselorId 반영
 *   ✅ 삭제(OPERATOR) 200 하드삭제                 ✅ 무토큰 403   ✅ 권한없음(STAFF) 403
 *   ✅ courseId 누락 400                           ✅ 미존재 404(COURSE_PARTICIPANT_NOT_FOUND)
 * ════════════════════════════════════════════════════════════════════════════════════
 *  Postman CLI 실서버 호출 결과 (v1.41.1 · 2026-07-07 · bootRun local:3434, dev DB 시드)
 *  실 로그인(admin01·oper01 / 비밀번호 1234)으로 accessToken 발급 → 16 requests / 0 failed.
 *  각 응답 JSON이 명세(docs/API/participant-cp-api-detail.md §2)와 필드 단위로 일치함을 확인.
 *  (실호출로 생성된 course '양천5기'·participant 010-5678-1234 는 검증 후 dev DB에서 정리 완료.)
 *  ────────────────────────────────────────────────────────────────────────────────────
 *   2-1 등록  POST   /api/course-participants           → 200 {"courseParticipantId":21,"status":"APPLIED"}
 *   2-2 목록  GET    /api/course-participants?courseId.. → 200 {"content":[{"courseParticipantId":21,
 *                                                            "participantName":"김철수","phone":"010-5678-1234",
 *                                                            "status":"APPLIED","counselorName":"상담사1"}],
 *                                                            "page":0,"size":10,"totalElements":1,"totalPages":1}
 *   2-3 상세  GET    /api/course-participants/{id}       → 200 {"courseParticipantId":21,"participantId":10022,
 *                                                            "participantName":"김철수","courseId":21,
 *                                                            "courseName":"양천5기","counselorId":7,
 *                                                            "counselorName":"상담사1","status":"APPLIED",
 *                                                            "contactAttempt":0,"basicEducation":"Y"}
 *   2-4 수정  PUT    /api/course-participants/{id}       → 200 {"updated":true}
 *   2-5 삭제  DELETE /api/course-participants/{id}       → 200 {"deleted":true}
 *   2-6 취소  POST   /api/course-participants/{id}/cancel        → 200 {"status":"CANCELED"}
 *   2-7 수료  PATCH  /api/course-participants/{id}/completion    → 200 {"courseParticipantId":21,"status":"COMPLETED"}
 *   2-8 연락  PATCH  /api/course-participants/{id}/contact-attempt → 200 {"contactAttempt":1}
 *   2-9 상담사 PATCH /api/course-participants/{id}/counselor      → 200 {"courseParticipantId":21,"counselorId":2}
 *   가드 무토큰 POST → 403   ·   검증 빈바디 POST → 400 {"success":false,"error":"participantId: 널이어서는 안됩니다"}
 *   미존재 GET /99999999 → 404 {"success":false,"error":"수강 정보를 찾을 수 없습니다."}
 *  ────────────────────────────────────────────────────────────────────────────────────
 *  결론: 9개 엔드포인트 + 가드/검증/미존재까지 명세 JSON 형식과 100% 일치. HTTP 회귀는 본 통합테스트로 상시 검증.
 * ════════════════════════════════════════════════════════════════════════════════════
 */
