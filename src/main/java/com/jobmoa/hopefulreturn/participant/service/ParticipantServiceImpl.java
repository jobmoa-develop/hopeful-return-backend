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
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        // 지역·회차를 함께 선택한 경우 같은 트랜잭션으로 수강 등록 — 실패 시 참여자 저장도 롤백된다.
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
            Integer courseNumber, Integer localCourseNumber, String status, Set<Long> allowedParticipantIds,
            LocalDate registerDateFrom, LocalDate registerDateTo) {
        int pageNumber = sanitizePage(page);
        int pageSize = sanitizeSize(size);
        String normalizedName = normalize(name);
        String normalizedPhone = normalize(phone);
        // 상위 지역(서울) 선택 시 산하 하위 지역 전체로 확장한다. null 이면 지역 필터 미적용.
        List<Long> regionIds = regionResolver.resolveRegionIds(regionId, parentRegionId);
        CourseParticipantStatus parsedStatus = parseStatus(status);
        boolean hasRoundFilter = regionIds != null || courseNumber != null || localCourseNumber != null;
        boolean hasRegisterDateFilter = registerDateFrom != null || registerDateTo != null;
        boolean hasStatusFilter = parsedStatus != null;
        // 최신 수강건 기준 필터(회차·등록일·상태) 중 하나라도 있으면, 그 판정을 위해
        // 참여자별 최신 수강건이 필요하다.
        boolean needsLatestEnrollmentFilter = hasRoundFilter || hasRegisterDateFilter || hasStatusFilter;

        // 목록은 항상 "지역(표시 순서) → 이름" 기준으로 정렬돼야 한다. 지역 정보는 참여자의 최신
        // 수강건(latestEnrollment)에서 나오므로, DB 페이지네이션만으로는 이 정렬을 할 수 없다.
        // 그래서 필터 여부와 상관없이 전체 참여자를 먼저 로드해 최신 수강건을 계산한 뒤 필터·정렬 →
        // 메모리에서 페이지를 자른다. (참여자 수가 매우 커지면 이 방식은 성능 부담이 커질 수 있다 —
        // 그 시점엔 region에 실제 정렬용 컬럼을 추가해 DB 쿼리 레벨 정렬로 옮기는 걸 권장.)
        List<ParticipantEntity> all = new ArrayList<>(
                findParticipants(Pageable.unpaged(), normalizedName, normalizedPhone).getContent());
        all.sort(Comparator.comparingLong(ParticipantEntity::getParticipantId));
        Map<Long, CourseParticipantEntity> latestByParticipant = latestCourseParticipants(all);
        Map<Long, Integer> regionOrder = regionResolver.buildChildRegionDisplayOrder();

        List<ParticipantEntity> filtered = all.stream()
                // 역할 스코프 — 배정 회차/상담 건에 해당하는 참여자만(관리자급이면 allowedParticipantIds == null).
                .filter(participant -> allowedParticipantIds == null
                        || allowedParticipantIds.contains(participant.getParticipantId()))
                // 회차·등록일·상태 필터는 지정됐을 때만 적용한다(모두 최신 수강건 기준).
                .filter(participant -> {
                    if (!needsLatestEnrollmentFilter) {
                        return true;
                    }
                    CourseParticipantEntity latest = latestByParticipant.get(participant.getParticipantId());
                    if (latest == null) {
                        return false;
                    }
                    if (hasRoundFilter && !matchesRound(latest, regionIds, courseNumber, localCourseNumber)) {
                        return false;
                    }
                    if (hasRegisterDateFilter && !matchesRegisterDate(latest, registerDateFrom, registerDateTo)) {
                        return false;
                    }
                    return !hasStatusFilter || matchesStatus(latest, parsedStatus);
                })
                // 정렬: 1순위 지역(RegionSelect 표시 순서), 2순위 이름. 최신 수강건이 없거나
                // 지역 매핑을 못 찾으면 맨 뒤로 보낸다.
                .sorted(Comparator
                        .comparingInt((ParticipantEntity p) ->
                                regionDisplayOrder(p, latestByParticipant, regionOrder))
                        .thenComparing(ParticipantEntity::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int total = filtered.size();
        int from = Math.min(pageNumber * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<ParticipantEntity> pageContent = filtered.subList(from, to);
        // 요약(상담사·출결)은 현재 페이지의 참여자에 대해서만 배치 조회해 만든다.
        Map<Long, CourseParticipantEntity> pageLatest = new HashMap<>();
        for (ParticipantEntity participant : pageContent) {
            CourseParticipantEntity cp = latestByParticipant.get(participant.getParticipantId());
            if (cp != null) {
                pageLatest.put(participant.getParticipantId(), cp);
            }
        }
        Map<Long, EnrollmentSummary> summaries = buildEnrollmentSummaries(pageLatest);
        List<ParticipantListResponse.Item> content = pageContent.stream()
                .map(participant -> toListItem(participant, summaries.get(participant.getParticipantId())))
                .toList();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new ParticipantListResponse(content, pageNumber, pageSize, total, totalPages);
    }

    /**
     * 참여자의 최신 수강건 지역을 {@code regionOrder} 맵 기준 순서 인덱스로 변환한다.
     * 최신 수강건이 없거나(회차 이력 없는 참여자), course·region 매핑이 없으면 맨 뒤로 정렬되도록
     * {@link Integer#MAX_VALUE}를 반환한다.
     */
    private int regionDisplayOrder(
            ParticipantEntity participant,
            Map<Long, CourseParticipantEntity> latestByParticipant,
            Map<Long, Integer> regionOrder) {
        CourseParticipantEntity latest = latestByParticipant.get(participant.getParticipantId());
        if (latest == null || latest.getCourse() == null) {
            return Integer.MAX_VALUE;
        }
        Long regionId = latest.getCourse().getRegionId();
        return regionOrder.getOrDefault(regionId, Integer.MAX_VALUE);
    }

    /**
     * 참여자별 최신 수강건(courseParticipantId가 가장 큰 행) 엔티티를 배치 조회한다(course 즉시 로딩).
     * 회차·상태 필터 판정과 요약 생성이 공유하는 1차 조회.
     */
    private Map<Long, CourseParticipantEntity> latestCourseParticipants(List<ParticipantEntity> participants) {
        if (participants.isEmpty()) {
            return Map.of();
        }
        List<Long> participantIds = participants.stream()
                .map(ParticipantEntity::getParticipantId)
                .toList();
        Map<Long, CourseParticipantEntity> latestByParticipant = new HashMap<>();
        for (CourseParticipantEntity cp : courseParticipantRepository.findWithCourseByParticipantIdIn(participantIds)) {
            latestByParticipant.merge(cp.getParticipantId(), cp, (a, b) ->
                    a.getCourseParticipantId() >= b.getCourseParticipantId() ? a : b);
        }
        return latestByParticipant;
    }

    /**
     * 최신 수강건 엔티티 맵으로부터 요약(상담사·출결 포함)을 만든다 — 대상 수강건에 대해 상담사·출결을
     * 배치 조회(2쿼리)해 N+1을 막는다.
     */
    private Map<Long, EnrollmentSummary> buildEnrollmentSummaries(
            Map<Long, CourseParticipantEntity> latestByParticipant) {
        if (latestByParticipant.isEmpty()) {
            return Map.of();
        }
        List<Long> courseParticipantIds = latestByParticipant.values().stream()
                .map(CourseParticipantEntity::getCourseParticipantId)
                .toList();
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
        latestByParticipant.forEach((participantId, cp) -> result.put(participantId, EnrollmentSummary.from(
                cp,
                counselorsByCp.getOrDefault(cp.getCourseParticipantId(), List.of()),
                attendedDaysByCp.getOrDefault(cp.getCourseParticipantId(), 0L))));
        return result;
    }

    /**
     * 참여자의 최신 수강건이 지정한 회차(지역+회차번호)에 해당하는지 판정한다.
     * 최신 수강건(회차)이 없으면 회차 필터에는 매칭되지 않는다.
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
        // regionIds == null 이면 지역 필터 미적용, 빈 목록이면(상위지역 산하 없음) 매칭 대상 없음.
        if (regionIds != null && !regionIds.contains(course.getRegionId())) {
            return false;
        }
        // 회차: 전체회차(courseNumber)·지역회차(localCourseNumber)를 각각 exact match(전달된 값만 적용).
        if (courseNumber != null && !courseNumber.equals(course.getCourseNumber())) {
            return false;
        }
        return localCourseNumber == null || localCourseNumber.equals(course.getLocalCourseNumber());
    }

    /**
     * 참여자의 최신 수강건 전산 등록일(course_participant.created_at)이 [from, to] 범위(포함)에 드는지 판정한다.
     * created_at 이 없으면 등록일 필터에는 매칭되지 않는다.
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

    /**
     * 참여자의 최신 수강건 진행상태가 지정한 상태와 일치하는지 판정한다.
     * 최신 수강건(회차)이 없으면 상태 필터에는 매칭되지 않는다.
     */
    private boolean matchesStatus(CourseParticipantEntity latest, CourseParticipantStatus status) {
        if (latest == null) {
            return false;
        }
        return latest.getStatus() == status;
    }

    /**
     * 진행상태 쿼리 파라미터를 enum으로 변환한다. 비어있으면 상태 필터 미적용(null).
     * 유효하지 않은 값이면 400(INVALID_STATUS)을 던진다.
     */
    private CourseParticipantStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return CourseParticipantStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
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
        // 역할 스코프 — 배정 외 참여자는 접근 거부(403). ID 직접 조회 우회를 차단한다.
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
        // course_participant 가 participant 를 FK 로 참조한다(ON DELETE CASCADE 없음).
        // 회차 등록 이력이 남아 있으면 삭제를 차단하고, 먼저 회차 등록을 정리하도록 안내한다.
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
            ParticipantEntity participant, EnrollmentSummary latestEnrollment) {
        return new ParticipantListResponse.Item(
                participant.getParticipantId(),
                participant.getName(),
                participant.getBirthYear(),
                participant.getPhone(),
                participant.getMatchKey(),
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