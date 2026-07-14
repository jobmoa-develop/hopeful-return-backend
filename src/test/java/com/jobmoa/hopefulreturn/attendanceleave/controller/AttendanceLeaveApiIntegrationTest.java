package com.jobmoa.hopefulreturn.attendanceleave.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmoa.hopefulreturn.attendance.entity.AttendanceEntity;
import com.jobmoa.hopefulreturn.attendance.entity.AttendanceStatus;
import com.jobmoa.hopefulreturn.attendance.repository.AttendanceRepository;
import com.jobmoa.hopefulreturn.attendanceleave.entity.AttendanceLeaveEntity;
import com.jobmoa.hopefulreturn.attendanceleave.repository.AttendanceLeaveRepository;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import com.jobmoa.hopefulreturn.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
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
 * 외출(AttendanceLeave) API HTTP 통합 테스트 — 실제 SecurityConfig·JwtAuthenticationFilter·
 * GlobalExceptionHandler·JPA(실 DB)까지 MockMvc로 검증한다.
 * 토큰은 실제 {@link JwtTokenProvider}로 발급(oper01/6, admin01/1, staff01/9 — V4 시드 기준).
 * course→participant→course_participant→attendance→attendance_leave 를 시드하며 @Transactional 롤백.
 *
 * 결과 요약·Postman 실호출 대조는 파일 하단 주석 참고.
 * 실행 조건: 실제 MSSQL 필요 → {@code DB_PASSWORD} 있을 때만 활성화(무DB CI 자동 스킵).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class AttendanceLeaveApiIntegrationTest {

    private static final String BASE = "/api/attendance-leaves";

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
    private AttendanceRepository attendanceRepository;
    @Autowired
    private AttendanceLeaveRepository attendanceLeaveRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private String opToken;
    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        opToken = jwtTokenProvider.createAccessToken(6L, "oper01", "OPERATOR");
        adminToken = jwtTokenProvider.createAccessToken(1L, "admin01", "ADMIN");
        staffToken = jwtTokenProvider.createAccessToken(9L, "staff01", "STAFF");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Long seedAttendance() {
        Long courseId = courseRepository.saveAndFlush(CourseEntity.builder()
                .regionId(1L).courseNumber(5).courseName("양천5기")
                .capacity(20).minimumCapacity(5).status(CourseStatus.PLANNED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build()).getCourseId();
        Long participantId = participantRepository.saveAndFlush(ParticipantEntity.builder()
                .name("김철수").birthYear(1978).phone("010-5678-1234").matchKey("KCS_1978_1234")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build()).getParticipantId();
        Long cpId = courseParticipantRepository.saveAndFlush(CourseParticipantEntity.builder()
                .courseId(courseId).participantId(participantId)
                .status(CourseParticipantStatus.APPLIED).contactAttempt(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build()).getCourseParticipantId();
        return attendanceRepository.saveAndFlush(AttendanceEntity.builder()
                .courseParticipantId(cpId).dayNo(1)
                .checkInTime(LocalTime.of(8, 55)).checkOutTime(LocalTime.of(18, 2))
                .status(AttendanceStatus.ATTEND).createdAt(LocalDateTime.now())
                .build()).getAttendanceId();
    }

    private Long seedLeave(Long attendanceId) {
        return attendanceLeaveRepository.saveAndFlush(AttendanceLeaveEntity.builder()
                .attendanceId(attendanceId).leaveTime(LocalTime.of(14, 30)).returnTime(LocalTime.of(15, 20))
                .reason("병원 진료").createdAt(LocalDateTime.now())
                .build()).getAttendanceLeaveId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 등록(OPERATOR) → attendanceLeaveId, attendanceId, reason, createdAt")
    void register_ok() throws Exception {
        Long attendanceId = seedAttendance();
        Map<String, Object> body = new HashMap<>();
        body.put("attendanceId", attendanceId);
        body.put("leaveTime", "14:30:00");
        body.put("returnTime", "15:20:00");
        body.put("reason", "병원 진료");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attendanceLeaveId").isNumber())
                .andExpect(jsonPath("$.data.attendanceId").value(attendanceId))
                .andExpect(jsonPath("$.data.reason").value("병원 진료"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    // ✅ PASS
    @Test
    @DisplayName("[404] 존재하지 않는 출석으로 등록 → ATTENDANCE_NOT_FOUND")
    void register_attendanceNotFound() throws Exception {
        Map<String, Object> body = Map.of("attendanceId", 99999999, "reason", "사유");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("출석 정보를 찾을 수 없습니다."));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 상세 조회(COUNSELOR) — participantName 조인")
    void detail_ok() throws Exception {
        Long id = seedLeave(seedAttendance());
        flushAndClear();

        mockMvc.perform(get(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(jwtTokenProvider.createAccessToken(7L, "counsel01", "COUNSELOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendanceLeaveId").value(id))
                .andExpect(jsonPath("$.data.participantName").value("김철수"))
                .andExpect(jsonPath("$.data.reason").value("병원 진료"));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 수정(OPERATOR) — 시각·사유 반영 + updatedAt, DB 반영")
    void update_ok() throws Exception {
        Long id = seedLeave(seedAttendance());
        Map<String, Object> body = Map.of("leaveTime", "14:20:00", "returnTime", "15:15:00", "reason", "병원 진료(시간 수정)");

        mockMvc.perform(put(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendanceLeaveId").value(id))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        flushAndClear();
        AttendanceLeaveEntity updated = attendanceLeaveRepository.findById(id).orElseThrow();
        assertThat(updated.getReason()).isEqualTo("병원 진료(시간 수정)");
        assertThat(updated.getLeaveTime()).isEqualTo(LocalTime.of(14, 20));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 삭제(ADMIN) — deleted=true, 하드 삭제로 DB 제거")
    void delete_ok() throws Exception {
        Long id = seedLeave(seedAttendance());

        mockMvc.perform(delete(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));

        flushAndClear();
        assertThat(attendanceLeaveRepository.findById(id)).isEmpty();
    }

    // ✅ PASS
    @Test
    @DisplayName("[401] 토큰 없이 등록 요청 → 인증 필요")
    void register_noToken_unauthorized() throws Exception {
        Map<String, Object> body = Map.of("attendanceId", 1, "reason", "사유");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    // ✅ PASS
    @Test
    @DisplayName("[403] 권한 없는 롤(STAFF)로 등록 요청 → 접근 차단")
    void register_wrongRole_forbidden() throws Exception {
        Map<String, Object> body = Map.of("attendanceId", 1, "reason", "사유");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // ✅ PASS
    @Test
    @DisplayName("[400] 등록 시 attendanceId 누락 → 입력 검증 실패")
    void register_missingAttendanceId_badRequest() throws Exception {
        Map<String, Object> body = Map.of("reason", "사유");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ✅ PASS
    @Test
    @DisplayName("[404] 존재하지 않는 조퇴·외출 상세 조회 → ATTENDANCE_LEAVE_NOT_FOUND")
    void detail_notFound() throws Exception {
        mockMvc.perform(get(BASE + "/99999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("조퇴·외출 정보를 찾을 수 없습니다."));
    }
}

/*
 * ════════════════════════════════════════════════════════════════════════════════════
 *  상세 통합 테스트 결과 (AttendanceLeaveApiIntegrationTest · 2026-07-07)
 *  실행: DB_PASSWORD=... ./gradlew test --tests "*AttendanceLeaveApiIntegrationTest" (실 DB, 롤백)
 *  결과: 9 tests / 9 passed / 0 skipped / 0 failures / 0 errors
 *  ────────────────────────────────────────────────────────────────────────────────────
 *   ✅ 등록(OPERATOR) 200 createdAt        ✅ 등록 출석없음 404(ATTENDANCE_NOT_FOUND)
 *   ✅ 상세(COUNSELOR) participantName 조인  ✅ 수정(OPERATOR) 200 updatedAt+DB반영
 *   ✅ 삭제(ADMIN) 200 하드삭제             ✅ 무토큰 403   ✅ 권한없음(STAFF) 403
 *   ✅ attendanceId 누락 400               ✅ 미존재 404(ATTENDANCE_LEAVE_NOT_FOUND)
 * ════════════════════════════════════════════════════════════════════════════════════
 *  Postman CLI 실서버 호출 결과 (v1.41.1 · 2026-07-07 · bootRun local:3434, dev DB 시드)
 *  실 로그인(admin01·oper01·staff01 / 1234) → 15 requests / 0 failed. 응답 JSON 명세 §외출 필드 일치.
 *  (course→participant→course_participant→attendance→attendance_leave 시드, 검증 후 dev DB 정리 완료.)
 *  ※ curl 인라인 호출은 Windows 셸의 한글 바디 인코딩 문제(Invalid UTF-8)로 부적합 → Postman 컬렉션(UTF-8 파일)로 검증.
 *  ────────────────────────────────────────────────────────────────────────────────────
 *   등록  POST /api/attendance-leaves (OPERATOR) → 200
 *     {"attendanceLeaveId":5,"attendanceId":18,"leaveTime":"14:30:00","returnTime":"15:20:00",
 *      "reason":"병원 진료","createdAt":"2026-07-07T14:15:50.54"}
 *   상세  GET  /api/attendance-leaves/{id} (COUNSELOR) → 200
 *     {"attendanceLeaveId":5,"attendanceId":18,"participantName":"김철수","leaveTime":"14:30:00",
 *      "returnTime":"15:20:00","reason":"병원 진료"}
 *   수정  PUT  /api/attendance-leaves/{id} (OPERATOR) → 200
 *     {"attendanceLeaveId":5,"updatedAt":"2026-07-07T14:15:50.75"}
 *   삭제  DELETE /api/attendance-leaves/{id} (ADMIN) → 200 {"deleted":true}
 *   가드 무토큰 403 · 권한없음(STAFF) 403 · 검증 400 {"success":false,"error":"attendanceId: 널이어서는 안됩니다"}
 *   미존재 404 {"success":false,"error":"조퇴·외출 정보를 찾을 수 없습니다."}
 * ════════════════════════════════════════════════════════════════════════════════════
 */
