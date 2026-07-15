package com.jobmoa.hopefulreturn.attendance.controller;

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
 * 출결(Attendance) API HTTP 통합 테스트 — 실제 SecurityConfig(@PreAuthorize)·JwtAuthenticationFilter·
 * GlobalExceptionHandler·JPA(실 DB)까지 전 구간을 MockMvc로 검증한다.
 * 토큰은 실제 {@link JwtTokenProvider}로 발급(oper01/6, staff01/9, admin01/1, counsel01/7 — V4 시드 기준).
 * course/participant/course_participant/attendance 는 각 테스트에서 시드하며 @Transactional 로 롤백된다.
 *
 * 결과 요약·Postman 실호출 대조는 파일 하단 주석 참고.
 * 실행 조건: 실제 MSSQL 필요 → {@code DB_PASSWORD} 환경변수가 있을 때만 활성화(무DB CI 자동 스킵).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class AttendanceApiIntegrationTest {

    private static final String BASE = "/api/attendances";

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
    private String staffToken;
    private String adminToken;
    private String counselToken;

    @BeforeEach
    void setUp() {
        opToken = jwtTokenProvider.createAccessToken(6L, "oper01", "OPERATOR");
        staffToken = jwtTokenProvider.createAccessToken(9L, "staff01", "STAFF");
        adminToken = jwtTokenProvider.createAccessToken(1L, "admin01", "ADMIN");
        counselToken = jwtTokenProvider.createAccessToken(7L, "counsel01", "COUNSELOR");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Long seedCourse() {
        return courseRepository.saveAndFlush(CourseEntity.builder()
                .regionId(1L).courseNumber(5).localCourseNumber(1).courseName("양천5기")
                .capacity(20).minimumCapacity(5).status(CourseStatus.PLANNED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build()).getCourseId();
    }

    private Long seedParticipant(String phone) {
        return participantRepository.saveAndFlush(ParticipantEntity.builder()
                .name("김철수").birthYear(1978).phone(phone).matchKey("KCS_1978_" + phone.substring(phone.length() - 4))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build()).getParticipantId();
    }

    private Long seedCourseParticipant(Long courseId, Long participantId) {
        return courseParticipantRepository.saveAndFlush(CourseParticipantEntity.builder()
                .courseId(courseId).participantId(participantId)
                .status(CourseParticipantStatus.APPLIED).contactAttempt(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build()).getCourseParticipantId();
    }

    private Long seedAttendance(Long courseParticipantId) {
        return attendanceRepository.saveAndFlush(AttendanceEntity.builder()
                .courseParticipantId(courseParticipantId).dayNo(1)
                .checkInTime(LocalTime.of(8, 55, 23)).checkOutTime(LocalTime.of(18, 2, 10))
                .status(AttendanceStatus.ATTEND).createdAt(LocalDateTime.now())
                .build()).getAttendanceId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 등록(OPERATOR) → attendanceId 반환, status=ATTEND, createdAt")
    void register_ok() throws Exception {
        Long cpId = seedCourseParticipant(seedCourse(), seedParticipant("010-5678-1234"));
        Map<String, Object> body = new HashMap<>();
        body.put("courseParticipantId", cpId);
        body.put("dayNo", 1);
        body.put("checkInTime", "08:55:23");
        body.put("checkOutTime", "18:02:10");
        body.put("status", "ATTEND");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attendanceId").isNumber())
                .andExpect(jsonPath("$.data.status").value("ATTEND"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 일괄 등록(STAFF) → savedCount, dayNo, courseId, message")
    void bulk_ok() throws Exception {
        Long courseId = seedCourse();
        Long cp1 = seedCourseParticipant(courseId, seedParticipant("010-1111-1111"));
        Long cp2 = seedCourseParticipant(courseId, seedParticipant("010-2222-2222"));
        Map<String, Object> item1 = Map.of("courseParticipantId", cp1, "checkInTime", "08:55:10",
                "checkOutTime", "18:01:00", "status", "ATTEND");
        Map<String, Object> item2 = Map.of("courseParticipantId", cp2, "status", "ABSENT");
        Map<String, Object> body = Map.of("courseId", courseId, "dayNo", 1, "attendances", List.of(item1, item2));

        mockMvc.perform(post(BASE + "/bulk")
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savedCount").value(2))
                .andExpect(jsonPath("$.data.dayNo").value(1))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.message").value("출석 정보가 저장되었습니다."));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 목록 조회(OPERATOR) — courseId 필터 + participantName 조인")
    void list_ok() throws Exception {
        Long courseId = seedCourse();
        Long cpId = seedCourseParticipant(courseId, seedParticipant("010-5678-1234"));
        seedAttendance(cpId);
        flushAndClear();

        mockMvc.perform(get(BASE).param("courseId", String.valueOf(courseId)).param("dayNo", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].participantName").value("김철수"))
                .andExpect(jsonPath("$.data.content[0].dayNo").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("ATTEND"))
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    @DisplayName("[200] 목록 조회 — courseParticipantId 필터 + 항목에 courseParticipantId·조퇴/외출(leaves) 포함")
    void list_byCourseParticipant_withLeaves() throws Exception {
        Long courseId = seedCourse();
        Long cp1 = seedCourseParticipant(courseId, seedParticipant("010-1111-1111"));
        Long cp2 = seedCourseParticipant(courseId, seedParticipant("010-2222-2222"));
        Long attendanceId = seedAttendance(cp1);
        seedAttendance(cp2);
        attendanceLeaveRepository.saveAndFlush(AttendanceLeaveEntity.builder()
                .attendanceId(attendanceId)
                .leaveTime(LocalTime.of(14, 30))
                .returnTime(LocalTime.of(15, 20))
                .reason("병원 진료")
                .createdAt(LocalDateTime.now())
                .build());
        flushAndClear();

        mockMvc.perform(get(BASE).param("courseParticipantId", String.valueOf(cp1))
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].courseParticipantId").value(cp1))
                .andExpect(jsonPath("$.data.content[0].leaves.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].leaves[0].leaveTime").value("14:30:00"))
                .andExpect(jsonPath("$.data.content[0].leaves[0].returnTime").value("15:20:00"))
                .andExpect(jsonPath("$.data.content[0].leaves[0].reason").value("병원 진료"));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 상세 조회(COUNSELOR) — participantName 조인")
    void detail_ok() throws Exception {
        Long cpId = seedCourseParticipant(seedCourse(), seedParticipant("010-5678-1234"));
        Long id = seedAttendance(cpId);
        flushAndClear();

        mockMvc.perform(get(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(counselToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendanceId").value(id))
                .andExpect(jsonPath("$.data.participantName").value("김철수"))
                .andExpect(jsonPath("$.data.status").value("ATTEND"))
                .andExpect(jsonPath("$.data.dayNo").value(1));
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 수정(STAFF) — status 반영 + updatedAt, DB 반영")
    void update_ok() throws Exception {
        Long cpId = seedCourseParticipant(seedCourse(), seedParticipant("010-5678-1234"));
        Long id = seedAttendance(cpId);
        Map<String, Object> body = Map.of("checkInTime", "09:03:10", "checkOutTime", "18:00:00", "status", "LATE");

        mockMvc.perform(put(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendanceId").value(id))
                .andExpect(jsonPath("$.data.status").value("LATE"))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        flushAndClear();
        AttendanceEntity updated = attendanceRepository.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AttendanceStatus.LATE);
    }

    // ✅ PASS
    @Test
    @DisplayName("[200] 삭제(ADMIN) — deleted=true, 하드 삭제로 DB 제거")
    void delete_ok() throws Exception {
        Long cpId = seedCourseParticipant(seedCourse(), seedParticipant("010-5678-1234"));
        Long id = seedAttendance(cpId);

        mockMvc.perform(delete(BASE + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));

        flushAndClear();
        assertThat(attendanceRepository.findById(id)).isEmpty();
    }

    // ✅ PASS
    @Test
    @DisplayName("[401] 토큰 없이 등록 요청 → 인증 필요")
    void register_noToken_unauthorized() throws Exception {
        Map<String, Object> body = Map.of("courseParticipantId", 1, "dayNo", 1, "status", "ATTEND");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    // ✅ PASS
    @Test
    @DisplayName("[403] 권한 없는 롤(COUNSELOR)로 등록 요청 → 접근 차단")
    void register_wrongRole_forbidden() throws Exception {
        Map<String, Object> body = Map.of("courseParticipantId", 1, "dayNo", 1, "status", "ATTEND");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(counselToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // ✅ PASS
    @Test
    @DisplayName("[400] 등록 시 courseParticipantId 누락 → 입력 검증 실패")
    void register_missingCourseParticipantId_badRequest() throws Exception {
        Map<String, Object> body = Map.of("dayNo", 1, "status", "ATTEND");

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ✅ PASS
    @Test
    @DisplayName("[404] 존재하지 않는 출석 상세 조회 → ATTENDANCE_NOT_FOUND")
    void detail_notFound() throws Exception {
        mockMvc.perform(get(BASE + "/99999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(opToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("출석 정보를 찾을 수 없습니다."));
    }
}

/*
 * ════════════════════════════════════════════════════════════════════════════════════
 *  상세 통합 테스트 결과 (AttendanceApiIntegrationTest · 2026-07-07)
 *  실행: DB_PASSWORD=... ./gradlew test --tests "*AttendanceApiIntegrationTest" (실 DB, @Transactional 롤백)
 *  결과: 10 tests / 10 passed / 0 skipped / 0 failures / 0 errors
 *  ────────────────────────────────────────────────────────────────────────────────────
 *   ✅ 등록(OPERATOR) 200 status=ATTEND+createdAt   ✅ 일괄등록(STAFF) 200 savedCount=2
 *   ✅ 목록(OPERATOR) courseId필터+participantName    ✅ 상세(COUNSELOR) participantName 조인
 *   ✅ 수정(STAFF) 200 status=LATE+updatedAt+DB반영   ✅ 삭제(ADMIN) 200 하드삭제
 *   ✅ 무토큰 403   ✅ 권한없음(COUNSELOR) 403   ✅ courseParticipantId 누락 400
 *   ✅ 미존재 404(ATTENDANCE_NOT_FOUND)
 * ════════════════════════════════════════════════════════════════════════════════════
 *  Postman/실서버 호출 결과 (Postman CLI v1.41.1 + curl · 2026-07-07 · bootRun local:3434, dev DB 시드)
 *  실 로그인(admin01·oper01·staff01 / 1234)으로 accessToken 발급 → 15 requests / 0 failed.
 *  각 응답 JSON이 명세(Notion §출결)와 필드 단위로 일치. (생성 test data는 검증 후 dev DB 정리 완료.)
 *  ────────────────────────────────────────────────────────────────────────────────────
 *   등록  POST /api/attendances (OPERATOR) → 200
 *     {"attendanceId":10,"courseParticipantId":29,"dayNo":1,"checkInTime":"08:55:23",
 *      "checkOutTime":"18:02:10","status":"ATTEND","createdAt":"2026-07-07T13:41:44.19"}
 *   일괄  POST /api/attendances/bulk (STAFF) → 200
 *     {"savedCount":2,"dayNo":5,"courseId":28,"message":"출석 정보가 저장되었습니다."}
 *   목록  GET  /api/attendances?courseId=28&dayNo=1 (OPERATOR) → 200
 *     {"content":[{"attendanceId":10,"participantName":"김철수","dayNo":1,"checkInTime":"08:55:23",
 *      "checkOutTime":"18:02:10","status":"ATTEND"}],"page":0,"size":20,"totalElements":1,"totalPages":1}
 *   상세  GET  /api/attendances/{id} (COUNSELOR) → 200
 *     {"attendanceId":11,"courseParticipantId":29,"participantName":"김철수","dayNo":3,
 *      "checkInTime":"08:50:00","checkOutTime":"18:00:00","status":"ATTEND","createdAt":"2026-07-07T13:41:44.24"}
 *   수정  PUT  /api/attendances/{id} (STAFF) → 200
 *     {"attendanceId":11,"status":"LATE","updatedAt":"2026-07-07T13:41:44.42"}
 *   삭제  DELETE /api/attendances/{id} (ADMIN) → 200 {"deleted":true}
 *   가드 무토큰 403 · 검증 400 {"success":false,"error":"courseParticipantId: 널이어서는 안됩니다"}
 *   미존재 404 {"success":false,"error":"출석 정보를 찾을 수 없습니다."}
 * ════════════════════════════════════════════════════════════════════════════════════
 */
