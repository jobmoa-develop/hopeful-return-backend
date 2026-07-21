package com.jobmoa.hopefulreturn.courseparticipant.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportCommitRequest;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.security.JwtTokenProvider;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참여자 일괄 등록(XLSX) API HTTP 통합 테스트 — SecurityConfig(@PreAuthorize)·JwtAuthenticationFilter·
 * 멀티파트(preview)·JSON(commit) 바인딩·JPA(실 DB)까지 전 구간을 검증한다. @Transactional 로 롤백된다.
 * 실 MSSQL 필요 → {@code DB_PASSWORD} 환경변수가 있을 때만 활성화(무DB CI 자동 스킵).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class ParticipantBulkImportApiIntegrationTest {

    private static final String PREVIEW = "/api/course-participants/bulk-import/preview";
    private static final String COMMIT = "/api/course-participants/bulk-import/commit";
    private static final String COURSE_NAME = "[현장] (서울)IT일괄등록테스트_99회차";
    private static final String XLSX_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseParticipantRepository courseParticipantRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtTokenProvider.createAccessToken(1L, "admin01", "ADMIN");
    }

    @Test
    @DisplayName("미리보기: 업로드 파일을 교육과정명별 그룹으로 반환한다(쓰기 없음)")
    void previewReturnsGroups() throws Exception {
        MockMultipartFile file = xlsxTwoRows();

        mockMvc.perform(multipart(PREVIEW).file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(2))
                .andExpect(jsonPath("$.data.groups[0].sourceCourseName").value(COURSE_NAME))
                .andExpect(jsonPath("$.data.groups[0].roundNumber").value(99))
                .andExpect(jsonPath("$.data.groups[0].participantCount").value(2));
    }

    @Test
    @DisplayName("커밋: 편집된 행을 매핑된 회차에 등록한다")
    void commitRegistersMappedItems() throws Exception {
        Long courseId = seedCourse();
        long before = courseParticipantRepository.countByCourseId(courseId);

        BulkImportCommitRequest request = new BulkImportCommitRequest(List.of(
                item(1, courseId, "홍길동", "01099990001"),
                item(2, courseId, "김철수", "01099990002")));

        mockMvc.perform(post(COMMIT)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registeredCount").value(2))
                .andExpect(jsonPath("$.data.createdParticipantCount").value(2));

        long after = courseParticipantRepository.countByCourseId(courseId);
        org.assertj.core.api.Assertions.assertThat(after - before).isEqualTo(2);
    }

    @Test
    @DisplayName("커밋: targetCourseId 가 없으면 전부 미매핑으로 스킵되고 등록 0")
    void commitSkipsWhenNoTargetCourse() throws Exception {
        BulkImportCommitRequest request = new BulkImportCommitRequest(List.of(
                item(1, null, "홍길동", "01099990001"),
                item(2, null, "김철수", "01099990002")));

        mockMvc.perform(post(COMMIT)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registeredCount").value(0))
                .andExpect(jsonPath("$.data.skippedUnmappedCount").value(2));
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private BulkImportCommitRequest.Item item(int rowNumber, Long targetCourseId, String name, String phone) {
        return new BulkImportCommitRequest.Item(
                rowNumber, COURSE_NAME, targetCourseId, name, phone, 1986,
                LocalDate.of(2026, 7, 9), LocalDate.of(2026, 7, 9), "CONFIRMED");
    }

    private Long seedCourse() {
        CourseEntity course = CourseEntity.builder()
                .regionId(1L) // 서울(V6 시드)
                .courseNumber(99)
                .localCourseNumber(99) // V7 NOT NULL
                .courseName("일괄등록 테스트 회차")
                .capacity(30)
                .minimumCapacity(5)
                .status(CourseStatus.PLANNED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return courseRepository.saveAndFlush(course).getCourseId();
    }

    private MockMultipartFile xlsxTwoRows() {
        String[] headers = {
            "교육과정명", "교육기관소재지_시도", "교육기관소재지_시군구", "교육생명",
            "생년월일", "휴대폰번호", "신청일시", "접수진행상태", "선정여부", "선정일시"
        };
        String[][] rows = {
            {COURSE_NAME, "서울특별시", "서울특별시 양천구", "홍길동",
                "19860313", "01099990001", "2026-07-09 10:45:46.0", "접수완료", "선정", "2026-07-09"},
            {COURSE_NAME, "서울특별시", "서울특별시 양천구", "김철수",
                "19780101", "01099990002", "2026-07-09 11:00:00.0", "접수완료", "선정", "2026-07-09"}
        };
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("data");
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                headerRow.createCell(c).setCellValue(headers[c]);
            }
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            workbook.write(out);
            return new MockMultipartFile("file", "participants.xlsx", XLSX_TYPE, out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("테스트 xlsx 생성 실패", e);
        }
    }
}
