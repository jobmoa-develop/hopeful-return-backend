package com.jobmoa.hopefulreturn.followup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.courseparticipant.service.CourseParticipantService;
import com.jobmoa.hopefulreturn.followup.entity.FollowUpEntity;
import com.jobmoa.hopefulreturn.followup.model.dto.CreateFollowUpRequest;
import com.jobmoa.hopefulreturn.followup.model.dto.CreateFollowUpResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpDetailResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpListResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.UpdateFollowUpRequest;
import com.jobmoa.hopefulreturn.followup.model.dto.UpdateFollowUpResponse;
import com.jobmoa.hopefulreturn.followup.repository.FollowUpRepository;
import com.jobmoa.hopefulreturn.followupcounsel.entity.FollowUpCounselEntity;
import com.jobmoa.hopefulreturn.followupcounsel.repository.FollowUpCounselRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 사후관리(FollowUp) 서비스 단위 테스트 — 집계 목록(수료 위임 + 스냅샷/상담 enrich) + CRUD +
 * 상담사 배정 스코프(상세) 검증.
 */
@ExtendWith(MockitoExtension.class)
class FollowUpServiceImplTest {

    private static final Long CP_ID = 15L;
    private static final Long COUNSELOR_ID = 7L;

    @Mock
    private FollowUpRepository followUpRepository;
    @Mock
    private FollowUpCounselRepository followUpCounselRepository;
    @Mock
    private CourseParticipantRepository courseParticipantRepository;
    @Mock
    private CourseStaffRepository courseStaffRepository;
    @Mock
    private CourseParticipantService courseParticipantService;

    @InjectMocks
    private FollowUpServiceImpl service;

    private FollowUpEntity entity(Long id) {
        return FollowUpEntity.builder()
                .followupId(id)
                .courseParticipantId(CP_ID)
                .employmentDate(LocalDate.of(2026, 9, 24))
                .forestProgramDate(LocalDate.of(2026, 10, 5))
                .nationalProgramDate(LocalDate.of(2026, 10, 20))
                .nationalProgramBranch("남부")
                .build();
    }

    private CourseParticipantListResponse onePageCompleted() {
        CourseParticipantListResponse.Item cpItem = new CourseParticipantListResponse.Item(
                CP_ID, "김서연", "김서연_010****1023", null, "남부", "남부3기", 3, 3, "COMPLETED", List.of());
        return new CourseParticipantListResponse(List.of(cpItem), 0, 20, 1, 1);
    }

    private FollowUpCounselEntity counsel(int no, LocalDate date) {
        return FollowUpCounselEntity.builder()
                .courseParticipantId(CP_ID).counselNumber(no).counselDate(date).counselStatus("landline")
                .build();
    }

    @Test
    @DisplayName("등록: 신규 필드로 저장하고 followUpId·createdAt 을 반환한다")
    void create_success() {
        when(courseParticipantRepository.existsById(CP_ID)).thenReturn(true);
        when(followUpRepository.save(any(FollowUpEntity.class))).thenReturn(entity(11L));

        CreateFollowUpResponse res = service.create(new CreateFollowUpRequest(
                CP_ID, LocalDate.of(2026, 9, 24), LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 20), "남부"));

        assertThat(res.followUpId()).isEqualTo(11L);
        assertThat(res.courseParticipantId()).isEqualTo(CP_ID);
        assertThat(res.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("등록: 허용되지 않은 국취 지점이면 INVALID_INPUT")
    void create_invalidBranch() {
        when(courseParticipantRepository.existsById(CP_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateFollowUpRequest(
                CP_ID, null, null, null, "없는지점")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(followUpRepository, never()).save(any());
    }

    @Test
    @DisplayName("목록: 수료 위임 결과에 스냅샷+상담요약을 붙여 집계한다")
    void findAll_aggregates() {
        when(courseParticipantService.findAll(
                null, null, null, null, null, "COMPLETED", null, null, null, null, null, null, 0, 20))
                .thenReturn(onePageCompleted());
        when(courseParticipantRepository.findAllById(List.of(CP_ID)))
                .thenReturn(List.of(CourseParticipantEntity.builder()
                        .courseParticipantId(CP_ID).completionDate(LocalDate.of(2026, 5, 30)).build()));
        when(followUpRepository.findByCourseParticipantIdIn(List.of(CP_ID)))
                .thenReturn(List.of(entity(11L)));
        when(followUpCounselRepository.findByCourseParticipantIdIn(List.of(CP_ID)))
                .thenReturn(List.of(counsel(1, LocalDate.of(2026, 6, 26)), counsel(2, LocalDate.of(2026, 7, 10))));

        FollowUpListResponse res = service.findAll(null, null, null, null, null, null, null, null, 0, 20);

        assertThat(res.totalElements()).isEqualTo(1);
        FollowUpListResponse.Item item = res.content().get(0);
        assertThat(item.followUpId()).isEqualTo(11L);
        assertThat(item.name()).isEqualTo("김서연");
        assertThat(item.completionDate()).isEqualTo(LocalDate.of(2026, 5, 30));
        assertThat(item.employmentDate()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(item.counselCount()).isEqualTo(2L);
        assertThat(item.lastCounselDate()).isEqualTo(LocalDate.of(2026, 7, 10));
    }

    @Test
    @DisplayName("목록: 스냅샷 없는 수료자는 followUpId=null·상담수 0")
    void findAll_noSnapshot() {
        when(courseParticipantService.findAll(
                null, null, null, null, null, "COMPLETED", null, null, null, null, null, null, 0, 20))
                .thenReturn(onePageCompleted());
        when(courseParticipantRepository.findAllById(List.of(CP_ID)))
                .thenReturn(List.of(CourseParticipantEntity.builder()
                        .courseParticipantId(CP_ID).completionDate(LocalDate.of(2026, 5, 30)).build()));
        when(followUpRepository.findByCourseParticipantIdIn(List.of(CP_ID))).thenReturn(List.of());
        when(followUpCounselRepository.findByCourseParticipantIdIn(List.of(CP_ID))).thenReturn(List.of());

        FollowUpListResponse.Item item = service.findAll(null, null, null, null, null, null, null, null, 0, 20).content().get(0);

        assertThat(item.followUpId()).isNull();
        assertThat(item.employmentDate()).isNull();
        assertThat(item.counselCount()).isEqualTo(0L);
        assertThat(item.lastCounselDate()).isNull();
    }

    @Test
    @DisplayName("상세: 미존재면 FOLLOW_UP_NOT_FOUND")
    void findById_notFound() {
        when(followUpRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FOLLOW_UP_NOT_FOUND);
    }

    @Test
    @DisplayName("상세(상담사, 미배정): ACCESS_DENIED")
    void findById_counselorNotAssigned() {
        when(followUpRepository.findById(11L)).thenReturn(Optional.of(entity(11L)));
        when(courseParticipantRepository.findById(CP_ID))
                .thenReturn(Optional.of(CourseParticipantEntity.builder()
                        .courseParticipantId(CP_ID).courseId(100L).build()));
        when(courseStaffRepository.findByUserId(COUNSELOR_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.findById(11L, COUNSELOR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("상세 조회 매핑: 상세 응답이 신규 필드를 담는다")
    void findById_detailMapping() {
        when(followUpRepository.findById(11L)).thenReturn(Optional.of(entity(11L)));

        FollowUpDetailResponse res = service.findById(11L, null);

        assertThat(res.employmentDate()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(res.forestProgramDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(res.nationalProgramDate()).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    @DisplayName("수정: 신규 필드 반영 후 followUpId·updatedAt 반환")
    void update_success() {
        FollowUpEntity found = entity(11L);
        when(followUpRepository.findById(11L)).thenReturn(Optional.of(found));

        UpdateFollowUpResponse res = service.update(11L, new UpdateFollowUpRequest(
                LocalDate.of(2026, 9, 25), null, null, "관악"));

        assertThat(res.followUpId()).isEqualTo(11L);
        assertThat(res.updatedAt()).isNotNull();
        assertThat(found.getNationalProgramBranch()).isEqualTo("관악");
        assertThat(found.getEmploymentDate()).isEqualTo(LocalDate.of(2026, 9, 25));
    }

    @Test
    @DisplayName("삭제: 삭제 메시지를 반환한다")
    void delete_success() {
        when(followUpRepository.findById(11L)).thenReturn(Optional.of(entity(11L)));

        assertThat(service.delete(11L).message()).contains("삭제");
        verify(followUpRepository).delete(any(FollowUpEntity.class));
    }
}
