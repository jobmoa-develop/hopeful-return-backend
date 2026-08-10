package com.jobmoa.hopefulreturn.participant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobmoa.hopefulreturn.attendance.repository.AttendanceDayCount;
import com.jobmoa.hopefulreturn.attendance.repository.AttendanceRepository;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselingType;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorAssignment;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CreateCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.courseparticipant.service.CourseParticipantService;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;
import com.jobmoa.hopefulreturn.region.support.RegionResolver;
import com.jobmoa.hopefulreturn.participant.model.dto.CheckPhoneResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.CreateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantListResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.UpdateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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

    @Mock
    private CourseParticipantService courseParticipantService;

    @Mock
    private CourseParticipantRepository courseParticipantRepository;

    @Mock
    private CourseParticipantCounselorRepository courseParticipantCounselorRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private RegionResolver regionResolver;

    @InjectMocks
    private ParticipantServiceImpl participantService;

    @BeforeEach
    void defaultRegionResolver() {
        // 기본값: 지역 필터 없음(null). Mockito 는 List 반환을 빈 목록으로 기본 처리하므로 명시적으로 null 을 준다.
        // 지역 필터 테스트는 개별적으로 resolveRegionIds(regionId, parentRegionId) 를 재스텁한다.
        lenient().when(regionResolver.resolveRegionIds(any(), any())).thenReturn(null);
    }

    private ParticipantEntity participant(Long id, String name, String phone) {
        return ParticipantEntity.builder()
                .participantId(id)
                .name(name)
                .birthYear(1978)
                .phone(phone)
                .matchKey("KCS_1978_1234")
                .build();
    }

    // ✅ PASS (2026-07-06) · 0.669s
    @Test
    @DisplayName("등록 시 저장된 참여자 ID와 matchKey를 반환하고, 수강 등록은 호출하지 않는다")
    void create_returnsGeneratedId() {
        // Arrange
        CreateParticipantRequest request = new CreateParticipantRequest("김철수", 1978, "010-5678-1234", null);
        when(participantRepository.save(any(ParticipantEntity.class)))
                .thenReturn(participant(25L, "김철수", "010-5678-1234"));

        // Act
        ParticipantCreatedResponse response = participantService.create(request);

        // Assert
        assertThat(response.participantId()).isEqualTo(25L);
        assertThat(response.matchKey()).isEqualTo("KCS_1978_1234");
        assertThat(response.courseParticipantId()).isNull();
        ArgumentCaptor<ParticipantEntity> captor = ArgumentCaptor.forClass(ParticipantEntity.class);
        verify(participantRepository).save(captor.capture());
        // matchKey = 이니셜(로마자)_생년_전화뒤4
        assertThat(captor.getValue().getMatchKey()).isEqualTo("KCS_1978_1234");
        verify(courseParticipantService, never()).create(any(), any());
    }

    @Test
    @DisplayName("지역·회차와 함께 등록하면 course_participant를 CONFIRMED(선정)로 같이 생성한다")
    void create_withEnrollment_createsCourseParticipantConfirmed() {
        // Arrange
        CreateParticipantRequest request = new CreateParticipantRequest(
                "김철수", 1978, "010-5678-1234",
                new CreateParticipantRequest.Enrollment(
                        15L, "워크넷", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2), "Y",
                        List.of(new CounselorAssignment(8L, "PRE_SESSION"))));
        when(participantRepository.save(any(ParticipantEntity.class)))
                .thenReturn(participant(25L, "김철수", "010-5678-1234"));
        when(courseParticipantService.create(any(CreateCourseParticipantRequest.class),
                eq(CourseParticipantStatus.CONFIRMED)))
                .thenReturn(new CourseParticipantCreatedResponse(101L, "CONFIRMED"));

        // Act
        ParticipantCreatedResponse response = participantService.create(request);

        // Assert
        assertThat(response.participantId()).isEqualTo(25L);
        assertThat(response.courseParticipantId()).isEqualTo(101L);
        ArgumentCaptor<CreateCourseParticipantRequest> captor =
                ArgumentCaptor.forClass(CreateCourseParticipantRequest.class);
        verify(courseParticipantService).create(captor.capture(), eq(CourseParticipantStatus.CONFIRMED));
        assertThat(captor.getValue().courseId()).isEqualTo(15L);
        assertThat(captor.getValue().participantId()).isEqualTo(25L);
        assertThat(captor.getValue().inflowType()).isEqualTo("워크넷");
        assertThat(captor.getValue().applyDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(captor.getValue().receptionDate()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(captor.getValue().basicEducation()).isEqualTo("Y");
        assertThat(captor.getValue().counselors()).hasSize(1);
        assertThat(captor.getValue().counselors().get(0).counselorId()).isEqualTo(8L);
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
        assertThatThrownBy(() -> participantService.findById(99L, null))
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
        ParticipantResponse response = participantService.findById(25L, null);

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

    @Test
    @DisplayName("완전 삭제: 회차 등록 이력이 있으면 PARTICIPANT_HAS_ENROLLMENTS 로 차단하고 삭제하지 않는다")
    void delete_blockedWhenEnrollmentsExist() {
        ParticipantEntity existing = participant(25L, "김철수", "010-5678-1234");
        when(participantRepository.findById(25L)).thenReturn(Optional.of(existing));
        when(courseParticipantRepository.existsByParticipantId(25L)).thenReturn(true);

        assertThatThrownBy(() -> participantService.delete(25L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PARTICIPANT_HAS_ENROLLMENTS);
        verify(participantRepository, never()).delete(existing);
    }

    // ✅ PASS (2026-07-06) · 0.010s
    @Test
    @DisplayName("목록 조회 시 페이지 메타와 항목을 매핑해 반환한다")
    void findAll_mapsPage() {
        // Arrange — 필터·정렬·페이징은 DB 쿼리가 처리하고, 서비스는 그 ID 순서대로 매핑한다.
        ParticipantEntity p = participant(25L, "김철수", "010-5678-1234");
        when(participantRepository.findFilteredParticipantIdsSorted(
                any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(25L), Pageable.ofSize(10), 1));
        when(participantRepository.findAllById(List.of(25L))).thenReturn(List.of(p));

        // Act
        ParticipantListResponse response = participantService.findAll(
                null, null, null, null, null, null, null, null, null, null, null);

        // Assert
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).participantId()).isEqualTo(25L);
        assertThat(response.content().get(0).name()).isEqualTo("김철수");
        assertThat(response.content().get(0).matchKey()).isEqualTo("KCS_1978_1234");
        assertThat(response.content().get(0).latestEnrollment()).isNull();
    }

    @Test
    @DisplayName("목록 조회 시 최신 수강건 요약(latestEnrollment)을 배치 조회로 매핑한다")
    void findAll_mapsLatestEnrollment() {
        // Arrange — 참여자 1명, 수강건 2건(최신 = courseParticipantId가 큰 102)
        ParticipantEntity p = participant(25L, "김철수", "010-5678-1234");
        when(participantRepository.findFilteredParticipantIdsSorted(
                any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(25L), Pageable.ofSize(10), 1));
        when(participantRepository.findAllById(List.of(25L))).thenReturn(List.of(p));

        RegionEntity region = RegionEntity.builder().regionId(1L).name("서울").build();
        CourseEntity course = CourseEntity.builder()
                .courseId(15L).courseName("양천5기").courseNumber(5).localCourseNumber(2)
                .day1Date(LocalDate.of(2026, 8, 10)).day2Date(LocalDate.of(2026, 8, 11))
                .region(region)
                .build();
        CourseParticipantEntity oldEnrollment = CourseParticipantEntity.builder()
                .courseParticipantId(101L).participantId(25L).courseId(15L)
                .status(CourseParticipantStatus.COMPLETED).course(course)
                .build();
        CourseParticipantEntity latestEnrollment = CourseParticipantEntity.builder()
                .courseParticipantId(102L).participantId(25L).courseId(15L)
                .status(CourseParticipantStatus.CONFIRMED).course(course)
                .build();
        when(courseParticipantRepository.findWithCourseByParticipantIdIn(anyCollection()))
                .thenReturn(List.of(oldEnrollment, latestEnrollment));

        CourseParticipantCounselorEntity preRow = CourseParticipantCounselorEntity.builder()
                .courseParticipantId(102L).counselorId(8L).status(CounselingType.PRE_SESSION)
                .counselingStartedAt(java.time.LocalDateTime.of(2026, 8, 5, 14, 0))
                .counselingEndedAt(java.time.LocalDateTime.of(2026, 8, 5, 15, 0))
                .build();
        when(courseParticipantCounselorRepository.findByCourseParticipantIdIn(anyCollection()))
                .thenReturn(List.of(preRow));

        AttendanceDayCount dayCount = new AttendanceDayCount() {
            @Override
            public Long getCourseParticipantId() {
                return 102L;
            }

            @Override
            public Long getAttendedDays() {
                return 2L;
            }
        };
        when(attendanceRepository.countAttendedDaysByCourseParticipantIdIn(anyCollection(), anyCollection()))
                .thenReturn(List.of(dayCount));

        // Act
        ParticipantListResponse response = participantService.findAll(
                null, null, null, null, null, null, null, null, null, null, null);

        // Assert — 최신 수강건(102) 기준으로 지역/회차·사전상담 완료·출결 집계가 매핑된다
        ParticipantListResponse.Item item = response.content().get(0);
        assertThat(item.latestEnrollment()).isNotNull();
        assertThat(item.latestEnrollment().courseParticipantId()).isEqualTo(102L);
        assertThat(item.latestEnrollment().regionName()).isEqualTo("서울");
        assertThat(item.latestEnrollment().localCourseNumber()).isEqualTo(2);
        assertThat(item.latestEnrollment().status()).isEqualTo("CONFIRMED");
        assertThat(item.latestEnrollment().preCounselingCompleted()).isTrue();
        assertThat(item.latestEnrollment().counselors()).hasSize(1);
        assertThat(item.latestEnrollment().attendedDays()).isEqualTo(2);
        assertThat(item.latestEnrollment().totalCourseDays()).isEqualTo(2);
    }

    @Test
    @DisplayName("회차(regionId) 필터 시 최신 수강건이 해당 지역인 참여자만 반환한다")
    void findAll_roundFilter_byRegion() {
        // Arrange — 서울(regionId=1) 필터. 지역 필터·정렬은 DB 쿼리 책임이므로 쿼리가 25L 만 반환하도록 스텁하고,
        // 서비스가 resolveRegionIds 결과를 쿼리 파라미터(hasRegion=1, regionIds=[1L])로 올바르게 전달하는지 검증한다.
        ParticipantEntity p25 = participant(25L, "김철수", "010-5678-1234");
        when(participantRepository.findFilteredParticipantIdsSorted(
                any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(25L), Pageable.ofSize(10), 1));
        when(participantRepository.findAllById(List.of(25L))).thenReturn(List.of(p25));

        RegionEntity seoul = RegionEntity.builder().regionId(1L).name("서울").build();
        CourseEntity seoulCourse = CourseEntity.builder()
                .courseId(15L).courseName("서울5기").courseNumber(5).localCourseNumber(1)
                .regionId(1L).region(seoul).build();
        CourseParticipantEntity cp25 = CourseParticipantEntity.builder()
                .courseParticipantId(101L).participantId(25L).courseId(15L)
                .status(CourseParticipantStatus.CONFIRMED).course(seoulCourse).build();
        when(courseParticipantRepository.findWithCourseByParticipantIdIn(anyCollection()))
                .thenReturn(List.of(cp25));
        when(courseParticipantCounselorRepository.findByCourseParticipantIdIn(anyCollection()))
                .thenReturn(List.of());
        when(attendanceRepository.countAttendedDaysByCourseParticipantIdIn(anyCollection(), anyCollection()))
                .thenReturn(List.of());

        when(regionResolver.resolveRegionIds(1L, null)).thenReturn(List.of(1L));

        // Act — 서울(regionId=1) 회차 필터
        ParticipantListResponse response = participantService.findAll(
                0, 10, null, null, 1L, null, null, null, null, null, null);

        // Assert — 서울 소속 참여자만
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).participantId()).isEqualTo(25L);
        assertThat(response.content().get(0).latestEnrollment().regionName()).isEqualTo("서울");

        // 지역 필터가 쿼리 파라미터로 전달됐는지 확인: hasRegion=1, regionIds=[1L].
        ArgumentCaptor<Integer> hasRegionCaptor = ArgumentCaptor.forClass(Integer.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> regionIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(participantRepository).findFilteredParticipantIdsSorted(
                any(), any(), hasRegionCaptor.capture(), regionIdsCaptor.capture(),
                any(), any(), any(), any(), anyInt(), any(), any(Pageable.class));
        assertThat(hasRegionCaptor.getValue()).isEqualTo(1);
        assertThat(regionIdsCaptor.getValue()).containsExactly(1L);
    }

    @Test
    @DisplayName("회차 필터에 매칭되는 참여자가 없으면 빈 목록을 반환한다")
    void findAll_roundFilter_noMatch() {
        // 서울이지만 없는 회차번호(999) → DB 쿼리가 0건 반환 → 서비스는 빈 목록으로 조기 반환한다.
        when(participantRepository.findFilteredParticipantIdsSorted(
                any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.<Long>of(), Pageable.ofSize(10), 0));
        when(regionResolver.resolveRegionIds(1L, null)).thenReturn(List.of(1L));

        // Act — 서울(regionId=1) + 없는 회차번호(999)
        ParticipantListResponse response = participantService.findAll(
                0, 10, null, null, 1L, null, 999, null, null, null, null);

        assertThat(response.totalElements()).isZero();
        assertThat(response.content()).isEmpty();
    }

    @Test
    @DisplayName("역할 스코프가 있으면 그 참여자만 반환한다(진행자 배정 회차 참여자)")
    void findAll_scoped_onlyAllowedParticipants() {
        ParticipantEntity p25 = participant(25L, "김철수", "010-5678-1234");
        // 스코프 필터도 DB 쿼리 책임 — 허용 집합(25L)만 반환하도록 스텁하고, scopeOff·allowedIds 전달을 검증한다.
        when(participantRepository.findFilteredParticipantIdsSorted(
                any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(25L), Pageable.ofSize(10), 1));
        when(participantRepository.findAllById(List.of(25L))).thenReturn(List.of(p25));
        when(courseParticipantRepository.findWithCourseByParticipantIdIn(anyCollection()))
                .thenReturn(List.of());

        // 허용 스코프에 25L 만 포함.
        ParticipantListResponse response = participantService.findAll(
                0, 10, null, null, null, null, null, null, java.util.Set.of(25L), null, null);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).participantId()).isEqualTo(25L);

        // 스코프가 쿼리 파라미터로 강제됐는지 확인: scopeOff=0(스코프 있음), allowedIds=[25L].
        ArgumentCaptor<Integer> scopeOffCaptor = ArgumentCaptor.forClass(Integer.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> allowedIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(participantRepository).findFilteredParticipantIdsSorted(
                any(), any(), anyInt(), any(), any(), any(), any(), any(),
                scopeOffCaptor.capture(), allowedIdsCaptor.capture(), any(Pageable.class));
        assertThat(scopeOffCaptor.getValue()).isEqualTo(0);
        assertThat(allowedIdsCaptor.getValue()).containsExactly(25L);
    }

    @Test
    @DisplayName("등록일 필터 시 최신 수강건 created_at 이 범위(포함) 안인 참여자만 반환한다")
    void findAll_registerDateFilter_withinRange() {
        // Arrange — 등록일 필터도 DB 쿼리 책임. 쿼리가 범위 내(7/10 등록) 25L 만 반환하도록 스텁하고,
        // 서비스가 registerDateFrom/To 를 쿼리 파라미터로 그대로 전달하는지 검증한다.
        ParticipantEntity p25 = participant(25L, "김철수", "010-5678-1234");
        when(participantRepository.findFilteredParticipantIdsSorted(
                any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(25L), Pageable.ofSize(10), 1));
        when(participantRepository.findAllById(List.of(25L))).thenReturn(List.of(p25));

        RegionEntity seoul = RegionEntity.builder().regionId(1L).name("서울").build();
        CourseEntity course = CourseEntity.builder()
                .courseId(15L).courseName("서울5기").courseNumber(5).localCourseNumber(1)
                .regionId(1L).region(seoul).build();
        CourseParticipantEntity cp25 = CourseParticipantEntity.builder()
                .courseParticipantId(101L).participantId(25L).courseId(15L)
                .status(CourseParticipantStatus.CONFIRMED).course(course)
                .createdAt(java.time.LocalDateTime.of(2026, 7, 10, 9, 0)).build();
        when(courseParticipantRepository.findWithCourseByParticipantIdIn(anyCollection()))
                .thenReturn(List.of(cp25));
        when(courseParticipantCounselorRepository.findByCourseParticipantIdIn(anyCollection()))
                .thenReturn(List.of());
        when(attendanceRepository.countAttendedDaysByCourseParticipantIdIn(anyCollection(), anyCollection()))
                .thenReturn(List.of());

        // Act — 7/1 ~ 7/15 범위
        ParticipantListResponse response = participantService.findAll(
                0, 10, null, null, null, null, null, null, null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15));

        // Assert
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).participantId()).isEqualTo(25L);

        // 등록일 범위가 쿼리 파라미터로 전달됐는지 확인.
        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(participantRepository).findFilteredParticipantIdsSorted(
                any(), any(), anyInt(), any(), any(), any(),
                fromCaptor.capture(), toCaptor.capture(), anyInt(), any(), any(Pageable.class));
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(toCaptor.getValue()).isEqualTo(LocalDate.of(2026, 7, 15));
    }

    @Test
    @DisplayName("역할 스코프 밖 참여자 상세는 ACCESS_DENIED — 조회 전에 차단")
    void findById_outOfScope_accessDenied() {
        assertThatThrownBy(() -> participantService.findById(25L, java.util.Set.of(999L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(participantRepository, never()).findById(any());
    }

    @Test
    @DisplayName("등록일 필터의 종료일은 포함(inclusive) — 종료일 당일 등록 건도 반환한다")
    void findAll_registerDateFilter_toInclusive() {
        // 종료일 포함(inclusive) 판정은 이제 DB 쿼리(CAST(created_at AS DATE) <= :to) 책임이므로,
        // 단위 테스트는 서비스가 registerDateTo 를 쿼리에 그대로 전달하는지 검증한다.
        ParticipantEntity p25 = participant(25L, "김철수", "010-5678-1234");
        when(participantRepository.findFilteredParticipantIdsSorted(
                any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(25L), Pageable.ofSize(10), 1));
        when(participantRepository.findAllById(List.of(25L))).thenReturn(List.of(p25));
        when(courseParticipantRepository.findWithCourseByParticipantIdIn(anyCollection()))
                .thenReturn(List.of());

        // Act — to = 7/15
        ParticipantListResponse response = participantService.findAll(
                0, 10, null, null, null, null, null, null, null, null, LocalDate.of(2026, 7, 15));

        // Assert
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).participantId()).isEqualTo(25L);

        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(participantRepository).findFilteredParticipantIdsSorted(
                any(), any(), anyInt(), any(), any(), any(), any(),
                toCaptor.capture(), anyInt(), any(), any(Pageable.class));
        assertThat(toCaptor.getValue()).isEqualTo(LocalDate.of(2026, 7, 15));
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
