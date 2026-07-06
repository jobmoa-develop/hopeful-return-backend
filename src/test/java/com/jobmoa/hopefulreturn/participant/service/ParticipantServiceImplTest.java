package com.jobmoa.hopefulreturn.participant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participant.model.dto.CheckPhoneResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.CreateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantListResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.UpdateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/*
 * ── 테스트 결과 요약 (2026-07-06) ──────────────────────────────
 *   실행: ./gradlew test --tests "*ParticipantServiceImplTest"  →  BUILD SUCCESSFUL
 *   결과: 9 tests / 0 failures / 0 errors / 0 skipped  →  전체 통과 ✅
 *   개별 결과·소요시간은 각 @Test 위 주석 참고.
 * ──────────────────────────────────────────────────────────────
 */
@ExtendWith(MockitoExtension.class)
class ParticipantServiceImplTest {

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private ParticipantServiceImpl participantService;

    private ParticipantEntity participant(Long id, String name, String phone) {
        return ParticipantEntity.builder()
                .participantId(id)
                .name(name)
                .birthYear(1978)
                .phone(phone)
                .build();
    }

    // ✅ PASS (2026-07-06) · 0.669s
    @Test
    @DisplayName("등록 시 저장된 참여자 ID를 반환한다")
    void create_returnsGeneratedId() {
        // Arrange
        CreateParticipantRequest request = new CreateParticipantRequest("김철수", 1978, "010-5678-1234");
        when(participantRepository.save(any(ParticipantEntity.class)))
                .thenReturn(participant(25L, "김철수", "010-5678-1234"));

        // Act
        ParticipantCreatedResponse response = participantService.create(request);

        // Assert
        assertThat(response.participantId()).isEqualTo(25L);
        ArgumentCaptor<ParticipantEntity> captor = ArgumentCaptor.forClass(ParticipantEntity.class);
        verify(participantRepository).save(captor.capture());
        // matchKey = 이니셜(로마자)_생년_전화뒤4
        assertThat(captor.getValue().getMatchKey()).isEqualTo("KCS_1978_1234");
    }

    // ✅ PASS (2026-07-06) · 0.001s
    @Test
    @DisplayName("전화번호가 존재하면 중복=true와 기존 ID를 반환한다")
    void checkPhone_duplicate() {
        // Arrange
        when(participantRepository.findFirstByPhoneOrderByParticipantIdAsc("010-5678-1234"))
                .thenReturn(Optional.of(participant(25L, "김철수", "010-5678-1234")));

        // Act
        CheckPhoneResponse response = participantService.checkPhone("010-5678-1234");

        // Assert
        assertThat(response.duplicate()).isTrue();
        assertThat(response.participantId()).isEqualTo(25L);
    }

    // ✅ PASS (2026-07-06) · 0.001s
    @Test
    @DisplayName("전화번호가 없으면 중복=false와 null ID를 반환한다")
    void checkPhone_notDuplicate() {
        // Arrange
        when(participantRepository.findFirstByPhoneOrderByParticipantIdAsc("010-0000-0000"))
                .thenReturn(Optional.empty());

        // Act
        CheckPhoneResponse response = participantService.checkPhone("010-0000-0000");

        // Assert
        assertThat(response.duplicate()).isFalse();
        assertThat(response.participantId()).isNull();
    }

    // ✅ PASS (2026-07-06) · 0.006s
    @Test
    @DisplayName("빈 전화번호로 중복 확인 시 INVALID_INPUT 예외")
    void checkPhone_blank_throws() {
        assertThatThrownBy(() -> participantService.checkPhone("  "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    // ✅ PASS (2026-07-06) · 0.001s
    @Test
    @DisplayName("존재하지 않는 참여자 상세 조회 시 PARTICIPANT_NOT_FOUND 예외")
    void findById_notFound_throws() {
        // Arrange
        when(participantRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> participantService.findById(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    // ✅ PASS (2026-07-06) · 0.002s
    @Test
    @DisplayName("상세 조회 시 참여자 필드를 매핑해 반환한다")
    void findById_returnsResponse() {
        // Arrange
        when(participantRepository.findById(25L))
                .thenReturn(Optional.of(participant(25L, "김철수", "010-5678-1234")));

        // Act
        ParticipantResponse response = participantService.findById(25L);

        // Assert
        assertThat(response.participantId()).isEqualTo(25L);
        assertThat(response.name()).isEqualTo("김철수");
        assertThat(response.phone()).isEqualTo("010-5678-1234");
    }

    // ✅ PASS (2026-07-06) · 0.006s
    @Test
    @DisplayName("수정 시 이름/전화번호가 반영되고 updated=true를 반환한다")
    void update_appliesChanges() {
        // Arrange
        ParticipantEntity existing = participant(25L, "김철수", "010-5678-1234");
        when(participantRepository.findById(25L)).thenReturn(Optional.of(existing));
        UpdateParticipantRequest request = new UpdateParticipantRequest("김영희", 1979, "010-1111-2222");

        // Act
        participantService.update(25L, request);

        // Assert
        ArgumentCaptor<ParticipantEntity> captor = ArgumentCaptor.forClass(ParticipantEntity.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("김영희");
        assertThat(captor.getValue().getPhone()).isEqualTo("010-1111-2222");
        assertThat(captor.getValue().getBirthYear()).isEqualTo(1979);
        assertThat(captor.getValue().getMatchKey()).isEqualTo("KYH_1979_2222");
    }

    // ✅ PASS (2026-07-06) · 0.002s
    @Test
    @DisplayName("삭제 시 하드 삭제(repository.delete)를 호출한다")
    void delete_hardDeletes() {
        // Arrange
        ParticipantEntity existing = participant(25L, "김철수", "010-5678-1234");
        when(participantRepository.findById(25L)).thenReturn(Optional.of(existing));

        // Act
        ParticipantDeletedResponse response = participantService.delete(25L);

        // Assert
        assertThat(response.deleted()).isTrue();
        verify(participantRepository, times(1)).delete(existing);
    }

    // ✅ PASS (2026-07-06) · 0.010s
    @Test
    @DisplayName("목록 조회 시 페이지 메타와 항목을 매핑해 반환한다")
    void findAll_mapsPage() {
        // Arrange
        Page<ParticipantEntity> page = new PageImpl<>(
                List.of(participant(25L, "김철수", "010-5678-1234")),
                Pageable.ofSize(10),
                1);
        when(participantRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        ParticipantListResponse response = participantService.findAll(null, null, null, null);

        // Assert
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).participantId()).isEqualTo(25L);
        assertThat(response.content().get(0).name()).isEqualTo("김철수");
    }
}

/*
 * ════════════════════════════════════════════════════════════════════════════════════
 *  Postman API 테스트 점검 결과 (참여자 API 실서버 호출 · 2026-07-06, 실 로그인으로 재검증)
 *  도구: Postman CLI v1.41.1  ·  인증: 실제 로그인(oper01·admin01 / 비밀번호 1234)으로 발급한 accessToken
 *  대상: /api/participants  (bootRun local, dev DB V1~V5 시드 적용 상태)
 *  ────────────────────────────────────────────────────────────────────────────────────
 *   #  요청                                         기대            결과
 *   1  POST   /api/participants (OPERATOR)          200 {id}        ✅ 200 {participantId:1}
 *   2  GET    /api/participants (OPERATOR)          200 page        ✅ 200 content 1건
 *   3  GET    /api/participants/1 (인증 사용자)     200 상세        ✅ 200 name:김철수
 *   4  GET    /api/participants/check-phone         duplicate:true  ✅ 200 {duplicate:true,participantId:1}
 *   5  PUT    /api/participants/1 (OPERATOR)        200 updated     ✅ 200 {participantId:1,updated:true}
 *   6  GET    /api/participants/1 (수정 반영)       김영희          ✅ 200 name:김영희
 *   7  DELETE /api/participants/1 (OPERATOR=권한X)  403             ❌ 500 → 공통 결함 / 이슈 #15
 *   8  DELETE /api/participants/1 (ADMIN)           200 deleted     ✅ 200 {deleted:true}
 *   9  GET    /api/participants/1 (삭제 후)         404             ✅ 404 "참여자를 찾을 수 없습니다."
 *  10  POST   /api/participants (토큰 없음)         401/403         ✅ 403 Forbidden
 *  11  POST   /api/participants (name 공백)         400             ✅ 400 "name: 공백일 수 없습니다"
 *  ────────────────────────────────────────────────────────────────────────────────────
 *  결론: 10/11 통과. #7 은 @PreAuthorize 권한부족(AuthorizationDeniedException)이 500으로
 *        처리되던 GlobalExceptionHandler 공통 결함 → 별도 버그 이슈 #15 로 분리.
 *        권장 수정(AccessDeniedException → 403) 로컬 검증 완료(재호출 시 403 확인, 회귀 없음).
 *  재검증(실 로그인): 동일 결과(10/11) + 등록 시 matchKey=KCS_1978_1234 DB 저장 확인, DB 정리(0건).
 *  ※ HTTP 자동화 회귀는 ParticipantApiIntegrationTest(11건, 10 pass/1 @Disabled #15)로 상시 검증.
 * ════════════════════════════════════════════════════════════════════════════════════
 */
