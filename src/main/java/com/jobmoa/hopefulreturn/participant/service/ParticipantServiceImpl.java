package com.jobmoa.hopefulreturn.participant.service;

import com.jobmoa.hopefulreturn.attendance.entity.AttendanceStatus;
import com.jobmoa.hopefulreturn.attendance.repository.AttendanceDayCount;
import com.jobmoa.hopefulreturn.attendance.repository.AttendanceRepository;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CreateCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.courseparticipant.service.CourseParticipantService;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participant.model.dto.CheckPhoneResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.CreateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.model.dto.EnrollmentSummary;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantListResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantUpdatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.UpdateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantEnrollmentRef;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import com.jobmoa.hopefulreturn.participant.support.MatchKeyGenerator;
import com.jobmoa.hopefulreturn.region.support.RegionResolver;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantServiceImpl implements ParticipantService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final ParticipantRepository participantRepository;
    private final CourseParticipantService courseParticipantService;
    private final CourseParticipantRepository courseParticipantRepository;
    private final CourseParticipantCounselorRepository courseParticipantCounselorRepository;
    private final AttendanceRepository attendanceRepository;
    private final RegionResolver regionResolver;

    @Override
    public ParticipantCreatedResponse create(CreateParticipantRequest request) {
        LocalDateTime now = LocalDateTime.now();
        ParticipantEntity participant = ParticipantEntity.builder()
                .name(request.name())
                .birthYear(request.birthYear())
                .phone(request.phone())
                .matchKey(MatchKeyGenerator.generate(request.name(), request.birthYear(), request.phone()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        ParticipantEntity saved = participantRepository.save(participant);
        Long courseParticipantId = enrollIfRequested(request.enrollment(), saved.getParticipantId());
        return new ParticipantCreatedResponse(saved.getParticipantId(), saved.getMatchKey(), courseParticipantId);
    }

    private Long enrollIfRequested(CreateParticipantRequest.Enrollment enrollment, Long participantId) {
        if (enrollment == null) {
            return null;
        }
        CreateCourseParticipantRequest courseParticipantRequest = new CreateCourseParticipantRequest(
                enrollment.courseId(),
                participantId,
                enrollment.counselors(),
                enrollment.inflowType(),
                enrollment.applyDate(),
                enrollment.receptionDate(),
                enrollment.basicEducation());
        CourseParticipantCreatedResponse created =
                courseParticipantService.create(courseParticipantRequest, CourseParticipantStatus.CONFIRMED);
        return created.courseParticipantId();
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantListResponse findAll(
            Integer page, Integer size, String name, String phone, Long regionId, Long parentRegionId,
            Integer courseNumber, Integer localCourseNumber, Set<Long> allowedParticipantIds,
            LocalDate registerDateFrom, LocalDate registerDateTo, String sortBy, String sortOrder) {
        int pageNumber = sanitizePage(page);
        int pageSize = sanitizeSize(size);
        String normalizedName = normalize(name);
        String normalizedPhone = normalize(phone);

        List<Long> regionIds = regionResolver.resolveRegionIds(regionId, parentRegionId);
        if (regionIds != null && regionIds.isEmpty()) {
            return new ParticipantListResponse(List.of(), pageNumber, pageSize, 0L, 0);
        }
        if (allowedParticipantIds != null && allowedParticipantIds.isEmpty()) {
            return new ParticipantListResponse(List.of(), pageNumber, pageSize, 0L, 0);
        }

        // 네이티브 쿼리의 "IN ()" 빈 리스트 문법 오류를 피하기 위해, 필터 미적용 시 더미값을 채우고
        // hasRegion=0/scopeOff=1 플래그로 해당 조건절 자체를 우회시킨다.
        int hasRegion = regionIds != null ? 1 : 0;
        List<Long> regionIdsParam = (regionIds != null && !regionIds.isEmpty()) ? regionIds : List.of(-1L);
        int scopeOff = allowedParticipantIds == null ? 1 : 0;
        List<Long> allowedIdsParam = (allowedParticipantIds != null && !allowedParticipantIds.isEmpty())
                ? new ArrayList<>(allowedParticipantIds)
                : List.of(-1L);

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<ParticipantEnrollmentRef> refPage = participantRepository.findFilteredEnrollmentRefsSorted(
                normalizedName,
                normalizedPhone,
                hasRegion,
                regionIdsParam,
                courseNumber,
                localCourseNumber,
                registerDateFrom,
                registerDateTo,
                scopeOff,
                allowedIdsParam,
                sortBy,
                sortOrder,
                pageable);

        List<ParticipantEnrollmentRef> orderedRefs = refPage.getContent();
        if (orderedRefs.isEmpty()) {
            return new ParticipantListResponse(
                    List.of(), refPage.getNumber(), refPage.getSize(),
                    refPage.getTotalElements(), refPage.getTotalPages());
        }

        // 참여자 엔티티 배치 로드(순서 무관 map). 같은 참여자가 여러 수강건 행으로 나올 수 있어 distinct.
        List<Long> participantIds = orderedRefs.stream()
                .map(ParticipantEnrollmentRef::participantId)
                .distinct()
                .toList();
        Map<Long, ParticipantEntity> participantById = participantRepository.findAllById(participantIds).stream()
                .collect(Collectors.toMap(ParticipantEntity::getParticipantId, p -> p));

        // 수강건(course+region) 배치 로드 후 수강건 id 기준 요약 생성(상담사·출결 배치조회로 N+1 방지).
        List<Long> courseParticipantIds = orderedRefs.stream()
                .map(ParticipantEnrollmentRef::courseParticipantId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, CourseParticipantEntity> cpById = courseParticipantIds.isEmpty()
                ? Map.of()
                : courseParticipantRepository.findWithCourseByCourseParticipantIdIn(courseParticipantIds).stream()
                        .collect(Collectors.toMap(CourseParticipantEntity::getCourseParticipantId, cp -> cp));
        Map<Long, EnrollmentSummary> summariesByCp = buildEnrollmentSummaries(cpById);

        // 쿼리가 정한 정렬 순서(orderedRefs)대로 수강건마다 한 행을 만든다.
        List<ParticipantListResponse.Item> content = orderedRefs.stream()
                .map(ref -> {
                    ParticipantEntity participant = participantById.get(ref.participantId());
                    if (participant == null) {
                        return null;
                    }
                    EnrollmentSummary summary = ref.courseParticipantId() == null
                            ? null
                            : summariesByCp.get(ref.courseParticipantId());
                    return toListItem(participant, ref.courseParticipantId(), summary);
                })
                .filter(Objects::nonNull)
                .toList();

        return new ParticipantListResponse(
                content, refPage.getNumber(), refPage.getSize(),
                refPage.getTotalElements(), refPage.getTotalPages());
    }

    /**
     * 수강건 엔티티 맵(key=courseParticipantId)으로부터 요약(상담사·출결 포함)을 만든다 — 대상 수강건에
     * 대해 상담사·출결을 배치 조회(2쿼리)해 N+1을 막는다. 반환 맵의 key 도 courseParticipantId.
     */
    private Map<Long, EnrollmentSummary> buildEnrollmentSummaries(
            Map<Long, CourseParticipantEntity> cpById) {
        if (cpById.isEmpty()) {
            return Map.of();
        }
        List<Long> courseParticipantIds = new ArrayList<>(cpById.keySet());
        Map<Long, List<CourseParticipantCounselorEntity>> counselorsByCp =
                courseParticipantCounselorRepository.findByCourseParticipantIdIn(courseParticipantIds).stream()
                        .collect(Collectors.groupingBy(CourseParticipantCounselorEntity::getCourseParticipantId));
        Map<Long, Long> attendedDaysByCp = attendanceRepository
                .countAttendedDaysByCourseParticipantIdIn(
                        courseParticipantIds, List.of(AttendanceStatus.ATTEND, AttendanceStatus.LATE))
                .stream()
                .collect(Collectors.toMap(
                        AttendanceDayCount::getCourseParticipantId, AttendanceDayCount::getAttendedDays));

        Map<Long, EnrollmentSummary> result = new HashMap<>();
        cpById.forEach((courseParticipantId, cp) -> result.put(courseParticipantId, EnrollmentSummary.from(
                cp,
                counselorsByCp.getOrDefault(courseParticipantId, List.of()),
                attendedDaysByCp.getOrDefault(courseParticipantId, 0L))));
        return result;
    }

    /**
     * 참여자의 최신 수강건이 지정한 회차(지역+회차번호)에 해당하는지 판정한다.
     * (현재 findAll 경로에서는 사용되지 않지만, 다른 참조 대비 보존)
     */
    private boolean matchesRound(
            CourseParticipantEntity latest, List<Long> regionIds, Integer courseNumber, Integer localCourseNumber) {
        if (latest == null) {
            return false;
        }
        CourseEntity course = latest.getCourse();
        if (course == null) {
            return false;
        }
        if (regionIds != null && !regionIds.contains(course.getRegionId())) {
            return false;
        }
        if (courseNumber != null && !courseNumber.equals(course.getCourseNumber())) {
            return false;
        }
        return localCourseNumber == null || localCourseNumber.equals(course.getLocalCourseNumber());
    }

    /**
     * 참여자의 최신 수강건 전산 등록일(course_participant.created_at)이 [from, to] 범위(포함)에 드는지 판정한다.
     * (현재 findAll 경로에서는 사용되지 않지만, 다른 참조 대비 보존)
     */
    private boolean matchesRegisterDate(CourseParticipantEntity latest, LocalDate from, LocalDate to) {
        if (latest == null || latest.getCreatedAt() == null) {
            return false;
        }
        LocalDate created = latest.getCreatedAt().toLocalDate();
        if (from != null && created.isBefore(from)) {
            return false;
        }
        return to == null || !created.isAfter(to);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckPhoneResponse checkPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return participantRepository.findFirstByPhoneOrderByParticipantIdAsc(phone.trim())
                .map(existing -> new CheckPhoneResponse(true, existing.getParticipantId()))
                .orElseGet(() -> new CheckPhoneResponse(false, null));
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantResponse findById(Long participantId, Set<Long> allowedParticipantIds) {
        if (allowedParticipantIds != null && !allowedParticipantIds.contains(participantId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        ParticipantEntity participant = findParticipant(participantId);
        return new ParticipantResponse(
                participant.getParticipantId(),
                participant.getName(),
                participant.getBirthYear(),
                participant.getPhone(),
                participant.getCreatedAt());
    }

    @Override
    public ParticipantUpdatedResponse update(Long participantId, UpdateParticipantRequest request) {
        ParticipantEntity participant = findParticipant(participantId);
        participant.setName(request.name());
        participant.setBirthYear(request.birthYear());
        participant.setPhone(request.phone());
        participant.setMatchKey(MatchKeyGenerator.generate(request.name(), request.birthYear(), request.phone()));
        participant.setUpdatedAt(LocalDateTime.now());
        participantRepository.save(participant);
        return new ParticipantUpdatedResponse(participant.getParticipantId(), true);
    }

    @Override
    public ParticipantDeletedResponse delete(Long participantId) {
        ParticipantEntity participant = findParticipant(participantId);
        if (courseParticipantRepository.existsByParticipantId(participantId)) {
            throw new BusinessException(ErrorCode.PARTICIPANT_HAS_ENROLLMENTS);
        }
        participantRepository.delete(participant);
        return new ParticipantDeletedResponse(true);
    }

    private Page<ParticipantEntity> findParticipants(Pageable pageable, String name, String phone) {
        boolean hasName = StringUtils.hasText(name);
        boolean hasPhone = StringUtils.hasText(phone);

        if (hasName && hasPhone) {
            return participantRepository.findByNameContainingAndPhoneContaining(name, phone, pageable);
        }
        if (hasName) {
            return participantRepository.findByNameContaining(name, pageable);
        }
        if (hasPhone) {
            return participantRepository.findByPhoneContaining(phone, pageable);
        }
        return participantRepository.findAll(pageable);
    }

    private ParticipantEntity findParticipant(Long participantId) {
        return participantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }

    private ParticipantListResponse.Item toListItem(
            ParticipantEntity participant, Long courseParticipantId, EnrollmentSummary latestEnrollment) {
        return new ParticipantListResponse.Item(
                participant.getParticipantId(),
                participant.getName(),
                participant.getBirthYear(),
                participant.getPhone(),
                participant.getMatchKey(),
                courseParticipantId,
                latestEnrollment);
    }

    private int sanitizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int sanitizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
