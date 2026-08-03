package com.jobmoa.hopefulreturn.courseparticipant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportCommitRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportParsedRow;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportPreviewResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportResultResponse;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.courseparticipant.support.ParticipantExcelParser;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link ParticipantBulkImportServiceImpl} 단위 테스트 — 파서·리포지토리를 목으로 대체해
 * 미리보기 그룹핑과 커밋 로직(등록/미매핑·중복·오류 스킵, 참여자 find-or-create)을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ParticipantBulkImportServiceImplTest {

    private static final String COURSE_A = "[현장] (서울)리본(Re:Born)커리어_16회차";
    private static final String COURSE_B = "[현장] (인천)리본(Re:Born)커리어_23회차";

    private final MultipartFile file = new MockMultipartFile(
            "file", "p.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1});

    @Mock
    private ParticipantExcelParser parser;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private CourseParticipantRepository courseParticipantRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private ParticipantBulkImportServiceImpl service;

    @Test
    @DisplayName("미리보기: 교육과정명별로 그룹을 나누고 오류 행 수를 센다")
    void previewGroupsByCourseName() {
        when(parser.parse(any())).thenReturn(List.of(
                previewRow(1, COURSE_A, "홍길동", "01011112222", null),
                previewRow(2, COURSE_A, "김철수", "01033334444", null),
                previewRow(3, COURSE_B, "이영희", null, "휴대폰번호가 올바르지 않습니다.")));
        when(regionRepository.findByName(any())).thenReturn(List.<RegionEntity>of());

        BulkImportPreviewResponse response = service.preview(file);

        assertThat(response.totalRows()).isEqualTo(3);
        assertThat(response.validRows()).isEqualTo(2);
        assertThat(response.invalidRows()).isEqualTo(1);
        assertThat(response.groups()).hasSize(2);
        assertThat(response.groups().get(0).sourceCourseName()).isEqualTo(COURSE_A);
        assertThat(response.groups().get(0).roundNumber()).isEqualTo(16);
        assertThat(response.groups().get(0).participantCount()).isEqualTo(2);
        assertThat(response.groups().get(1).roundNumber()).isEqualTo(23);
        assertThat(response.groups().get(1).invalidCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("커밋: 매핑된 신규 참여자를 등록하고 참여자를 새로 만든다")
    void commitRegistersMappedNewParticipant() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(participantRepository.findByMatchKey(any())).thenReturn(Optional.empty());
        when(participantRepository.findFirstByPhoneOrderByParticipantIdAsc(any())).thenReturn(Optional.empty());
        when(participantRepository.save(any())).thenAnswer(inv -> {
            ParticipantEntity p = inv.getArgument(0);
            p.setParticipantId(100L);
            return p;
        });
        when(courseParticipantRepository.existsByCourseIdAndParticipantId(1L, 100L)).thenReturn(false);

        BulkImportResultResponse result = service.commit(
                request(item(1, COURSE_A, 1L, "홍길동", "010-1111-2222")));

        assertThat(result.registeredCount()).isEqualTo(1);
        assertThat(result.createdParticipantCount()).isEqualTo(1);
        assertThat(result.skippedUnmappedCount()).isZero();
        verify(courseParticipantRepository).save(any(CourseParticipantEntity.class));
    }

    @Test
    @DisplayName("커밋: targetCourseId 가 없으면 미매핑으로 스킵한다")
    void commitSkipsUnmapped() {
        BulkImportResultResponse result = service.commit(
                request(item(1, COURSE_B, null, "이영희", "01055556666")));

        assertThat(result.registeredCount()).isZero();
        assertThat(result.skippedUnmappedCount()).isEqualTo(1);
        verify(participantRepository, never()).save(any());
        verify(courseParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("커밋: 이미 같은 회차에 등록된 참여자는 스킵하고 기존 참여자를 재사용한다")
    void commitSkipsDuplicateEnrollment() {
        ParticipantEntity existing = ParticipantEntity.builder()
                .participantId(50L).name("홍길동").phone("01011112222").build();
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(participantRepository.findByMatchKey(any())).thenReturn(Optional.of(existing));
        when(courseParticipantRepository.existsByCourseIdAndParticipantId(1L, 50L)).thenReturn(true);

        BulkImportResultResponse result = service.commit(
                request(item(1, COURSE_A, 1L, "홍길동", "01011112222")));

        assertThat(result.registeredCount()).isZero();
        assertThat(result.skippedDuplicateCount()).isEqualTo(1);
        assertThat(result.reusedParticipantCount()).isEqualTo(1);
        verify(participantRepository, never()).save(any());
        verify(courseParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("커밋: 이름/전화가 없는 행은 INVALID 로 스킵한다")
    void commitSkipsInvalidRow() {
        BulkImportResultResponse result = service.commit(
                request(item(1, COURSE_A, 1L, "이름있음", null)));

        assertThat(result.invalidRowCount()).isEqualTo(1);
        assertThat(result.registeredCount()).isZero();
        verify(participantRepository, never()).save(any());
    }

    @Test
    @DisplayName("커밋: 존재하지 않는 courseId 는 미매핑으로 스킵한다")
    void commitSkipsWhenTargetCourseMissing() {
        when(courseRepository.existsById(999L)).thenReturn(false);

        BulkImportResultResponse result = service.commit(
                request(item(1, COURSE_A, 999L, "홍길동", "01011112222")));

        assertThat(result.skippedUnmappedCount()).isEqualTo(1);
        assertThat(result.registeredCount()).isZero();
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────

    private BulkImportParsedRow previewRow(int rowNumber, String courseName, String name, String phone, String error) {
        return new BulkImportParsedRow(
                rowNumber, courseName, "서울특별시", "서울특별시 양천구", name, phone,
                1986, LocalDate.of(2026, 7, 9), LocalDate.of(2026, 7, 9),
                error == null ? "선정" : null,
                error == null ? "CONFIRMED" : "APPLIED", error);
    }

    private BulkImportCommitRequest request(BulkImportCommitRequest.Item... items) {
        return new BulkImportCommitRequest(List.of(items));
    }

    private BulkImportCommitRequest.Item item(
            int rowNumber, String courseName, Long targetCourseId, String name, String phone) {
        return new BulkImportCommitRequest.Item(
                rowNumber, courseName, targetCourseId, name, phone, 1986,
                LocalDate.of(2026, 7, 9), LocalDate.of(2026, 7, 9), "CONFIRMED");
    }
}
