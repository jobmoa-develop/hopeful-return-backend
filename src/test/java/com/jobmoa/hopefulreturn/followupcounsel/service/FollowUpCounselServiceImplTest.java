package com.jobmoa.hopefulreturn.followupcounsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.followupcounsel.entity.FollowUpCounselEntity;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.CreateFollowUpCounselRequest;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselCreatedResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselDetailResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselUpdatedResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.UpdateFollowUpCounselRequest;
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
 * 사후관리 상담(FollowUpCounsel) 서비스 단위 테스트 — CRUD + counselStatus 검증 + 상담사 배정 스코프.
 */
@ExtendWith(MockitoExtension.class)
class FollowUpCounselServiceImplTest {

    private static final Long CP_ID = 15L;
    private static final Long COUNSELOR_ID = 7L;

    @Mock
    private FollowUpCounselRepository followUpCounselRepository;
    @Mock
    private CourseParticipantRepository courseParticipantRepository;
    @Mock
    private CourseParticipantCounselorRepository courseParticipantCounselorRepository;

    @InjectMocks
    private FollowUpCounselServiceImpl service;

    private FollowUpCounselEntity entity(Long id) {
        return FollowUpCounselEntity.builder()
                .followUpCounselId(id)
                .courseParticipantId(CP_ID)
                .counselNumber(1)
                .counselDate(LocalDate.of(2026, 9, 24))
                .counselStatus("landline")
                .counselMemo("현재 구직 중")
                .build();
    }

    @Test
    @DisplayName("등록: created/updated 타임스탬프 세팅 후 응답 반환")
    void create_success() {
        when(courseParticipantRepository.existsById(CP_ID)).thenReturn(true);
        when(followUpCounselRepository.save(any(FollowUpCounselEntity.class)))
                .thenAnswer(inv -> {
                    FollowUpCounselEntity e = inv.getArgument(0);
                    e.setFollowUpCounselId(7L);
                    return e;
                });

        FollowUpCounselCreatedResponse res = service.create(new CreateFollowUpCounselRequest(
                CP_ID, 1, LocalDate.of(2026, 9, 24), "landline", "현재 구직 중"));

        assertThat(res.followUpCounselId()).isEqualTo(7L);
        assertThat(res.counselNumber()).isEqualTo(1);
        assertThat(res.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("등록: 허용되지 않은 counselStatus 면 INVALID_INPUT")
    void create_invalidStatus() {
        when(courseParticipantRepository.existsById(CP_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateFollowUpCounselRequest(
                CP_ID, 1, LocalDate.of(2026, 9, 24), "email", "메모")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(followUpCounselRepository, never()).save(any());
    }

    @Test
    @DisplayName("목록(상담사, 배정됨): 통과 후 매핑")
    void findAll_counselorAssigned() {
        when(courseParticipantRepository.existsById(CP_ID)).thenReturn(true);
        when(courseParticipantCounselorRepository
                .existsByCourseParticipantIdAndCounselorId(CP_ID, COUNSELOR_ID)).thenReturn(true);
        when(followUpCounselRepository.findByCourseParticipantIdOrderByCounselNumberAsc(CP_ID))
                .thenReturn(List.of(entity(7L)));

        assertThat(service.findAll(CP_ID, COUNSELOR_ID).content()).hasSize(1);
    }

    @Test
    @DisplayName("목록(상담사, 미배정): ACCESS_DENIED")
    void findAll_counselorNotAssigned() {
        when(courseParticipantRepository.existsById(CP_ID)).thenReturn(true);
        when(courseParticipantCounselorRepository
                .existsByCourseParticipantIdAndCounselorId(CP_ID, COUNSELOR_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.findAll(CP_ID, COUNSELOR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("상세: 미존재면 FOLLOW_UP_COUNSEL_NOT_FOUND")
    void findById_notFound() {
        when(followUpCounselRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FOLLOW_UP_COUNSEL_NOT_FOUND);
    }

    @Test
    @DisplayName("상세: 상세 응답 필드 매핑")
    void findById_detailMapping() {
        when(followUpCounselRepository.findById(7L)).thenReturn(Optional.of(entity(7L)));

        FollowUpCounselDetailResponse res = service.findById(7L, null);

        assertThat(res.counselStatus()).isEqualTo("landline");
        assertThat(res.counselDate()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(res.counselMemo()).isEqualTo("현재 구직 중");
    }

    @Test
    @DisplayName("수정: 필드 반영 + updatedAt 갱신")
    void update_success() {
        FollowUpCounselEntity found = entity(7L);
        when(followUpCounselRepository.findById(7L)).thenReturn(Optional.of(found));

        FollowUpCounselUpdatedResponse res = service.update(7L, new UpdateFollowUpCounselRequest(
                LocalDate.of(2026, 9, 25), "text", "문자 발송 완료"));

        assertThat(res.followUpCounselId()).isEqualTo(7L);
        assertThat(res.updatedAt()).isNotNull();
        assertThat(found.getCounselStatus()).isEqualTo("text");
        assertThat(found.getCounselMemo()).isEqualTo("문자 발송 완료");
    }

    @Test
    @DisplayName("삭제: 삭제 메시지 반환")
    void delete_success() {
        when(followUpCounselRepository.findById(7L)).thenReturn(Optional.of(entity(7L)));

        assertThat(service.delete(7L).message()).contains("삭제");
        verify(followUpCounselRepository).delete(any(FollowUpCounselEntity.class));
    }
}
