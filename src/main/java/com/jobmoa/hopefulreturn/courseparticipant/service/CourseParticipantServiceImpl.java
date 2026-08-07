package com.jobmoa.hopefulreturn.courseparticipant.service;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.AssignSlotCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.AssignableCounselorResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkAssignCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkCompleteCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkCompletionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkCounselorAssignResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CancelCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ChangeCourseParticipantStatusRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ChangeCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CompleteCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ContactAttemptResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselingSessionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorAssignment;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorChangeHistoryResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorChangedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorSummary;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCanceledResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCompletionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantStatusChangedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantDetailResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantUpdatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CreateCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.RecordCounselingSessionRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.UpdateCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CounselorChangeHistoryRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.ChangeSubject;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselingType;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselorChangeHistoryEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselorChangeType;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.StaffRole;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import com.jobmoa.hopefulreturn.region.support.RegionResolver;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseParticipantServiceImpl implements CourseParticipantService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final CourseParticipantRepository courseParticipantRepository;
    private final CourseParticipantCounselorRepository courseParticipantCounselorRepository;
    private final CounselorChangeHistoryRepository counselorChangeHistoryRepository;
    private final CourseRepository courseRepository;
    private final ParticipantRepository participantRepository;
    private final UsersRepository usersRepository;
    private final CourseStaffRepository courseStaffRepository;
    private final RegionResolver regionResolver;

    @Override
    public CourseParticipantCreatedResponse create(CreateCourseParticipantRequest request) {
        return create(request, CourseParticipantStatus.APPLIED);
    }

    @Override
    public CourseParticipantCreatedResponse create(
            CreateCourseParticipantRequest request, CourseParticipantStatus initialStatus) {
        validateCourseExists(request.courseId());
        validateParticipantExists(request.participantId());

        LocalDateTime now = LocalDateTime.now();
        List<CourseParticipantCounselorEntity> counselorRows =
                buildValidatedCounselorRows(null, request.counselors(), now);

        CourseParticipantEntity entity = CourseParticipantEntity.builder()
                .courseId(request.courseId())
                .participantId(request.participantId())
                .inflowType(request.inflowType())
                .applyDate(request.applyDate())
                .receptionDate(request.receptionDate())
                .basicEducation(request.basicEducation())
                .status(initialStatus)
                .contactAttempt(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        CourseParticipantEntity saved = courseParticipantRepository.save(entity);
        if (!counselorRows.isEmpty()) {
            counselorRows.forEach(row -> row.setCourseParticipantId(saved.getCourseParticipantId()));
            courseParticipantCounselorRepository.saveAll(counselorRows);
        }
        return new CourseParticipantCreatedResponse(saved.getCourseParticipantId(), saved.getStatus().name());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseParticipantListResponse findAll(
            Long courseId,
            Long regionId,
            Long parentRegionId,
            Integer courseNumber,
            Integer localCourseNumber,
            String status,
            String keyword,
            Set<Long> allowedCourseParticipantIds,
            java.time.LocalDate registerDateFrom,
            java.time.LocalDate registerDateTo,
            Integer page,
            Integer size) {
        int pageNumber = sanitizePage(page);
        int pageSize = sanitizeSize(size);
        CourseParticipantStatus parsedStatus = parseStatus(status);
        String normalizedKeyword = normalize(keyword);
        // 상위 지역(서울) 선택 시 산하 하위 지역 전체로 확장. null 이면 지역 필터 미적용,
        // 빈 목록(상위지역 산하 없음)이면 대상 없음 → 조기 0건 반환(참여자·문자·상담일정과 동일 규칙).
        List<Long> regionIds = regionResolver.resolveRegionIds(regionId, parentRegionId);
        if (regionIds != null && regionIds.isEmpty()) {
            return new CourseParticipantListResponse(List.of(), pageNumber, pageSize, 0L, 0);
        }
        if (allowedCourseParticipantIds != null && allowedCourseParticipantIds.isEmpty()) {
            return new CourseParticipantListResponse(List.of(), pageNumber, pageSize, 0L, 0);
        }

        // 네이티브 쿼리의 "IN ()" 빈 리스트 문법 오류를 피하기 위해, 필터 미적용 시 더미값을 채우고
        // hasRegion=0/scopeOff=1 플래그로 해당 조건절 자체를 우회시킨다.
        int hasRegion = regionIds != null ? 1 : 0;
        List<Long> regionIdsParam = (regionIds != null && !regionIds.isEmpty()) ? regionIds : List.of(-1L);
        int scopeOff = allowedCourseParticipantIds == null ? 1 : 0;
        List<Long> allowedIdsParam = (allowedCourseParticipantIds != null && !allowedCourseParticipantIds.isEmpty())
                ? new ArrayList<>(allowedCourseParticipantIds)
                : List.of(-1L);

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Long> idPage = courseParticipantRepository.findFilteredCourseParticipantIdsSorted(
                courseId,
                parsedStatus == null ? null : parsedStatus.name(),
                hasRegion,
                regionIdsParam,
                courseNumber,
                localCourseNumber,
                normalizedKeyword,
                registerDateFrom,
                registerDateTo,
                scopeOff,
                allowedIdsParam,
                pageable);

        List<Long> orderedIds = idPage.getContent();
        if (orderedIds.isEmpty()) {
            return new CourseParticipantListResponse(
                    List.of(), idPage.getNumber(), idPage.getSize(), idPage.getTotalElements(), idPage.getTotalPages());
        }

        // findWithParticipantAndCourseByCourseParticipantIdIn 은 반환 순서를 보장하지 않으므로,
        // 쿼리가 정한 정렬 순서(orderedIds)대로 재배열한다.
        Map<Long, CourseParticipantEntity> byId = courseParticipantRepository
                .findWithParticipantAndCourseByCourseParticipantIdIn(orderedIds).stream()
                .collect(Collectors.toMap(CourseParticipantEntity::getCourseParticipantId, cp -> cp));
        List<CourseParticipantEntity> pageContent = orderedIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();

        List<CourseParticipantListResponse.Item> content = pageContent.stream()
                .map(this::toListItem)
                .toList();

        return new CourseParticipantListResponse(
                content, idPage.getNumber(), idPage.getSize(), idPage.getTotalElements(), idPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseParticipantDetailResponse findById(Long courseParticipantId, Set<Long> allowedCourseParticipantIds) {
        // 역할 스코프 — 배정 외 수강건은 접근 거부(403). 서버측에서 ID 직접 조회 우회를 차단한다.
        if (allowedCourseParticipantIds != null && !allowedCourseParticipantIds.contains(courseParticipantId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        ParticipantEntity participant = entity.getParticipant();
        CourseEntity course = entity.getCourse();
        return new CourseParticipantDetailResponse(
                entity.getCourseParticipantId(),
                entity.getParticipantId(),
                participantName(entity),
                participant == null ? null : participant.getMatchKey(),
                participant == null ? null : participant.getBirthYear(),
                participantPhone(entity),
                entity.getCourseId(),
                courseName(entity),
                regionName(course),
                course == null ? null : course.getCourseNumber(),
                course == null ? null : course.getLocalCourseNumber(),
                counselorSummaries(entity),
                statusName(entity),
                entity.getContactAttempt(),
                entity.getInflowType(),
                entity.getApplyDate(),
                entity.getReceptionDate(),
                entity.getCompletionDate(),
                entity.getBasicEducation());
    }

    private String regionName(CourseEntity course) {
        if (course == null || course.getRegion() == null) {
            return null;
        }
        return course.getRegion().getName();
    }

    @Override
    public CourseParticipantUpdatedResponse update(Long courseParticipantId, UpdateCourseParticipantRequest request) {
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        LocalDateTime now = LocalDateTime.now();

        if (request.counselors() != null) {
            replaceCounselors(entity.getCourseParticipantId(), request.counselors(), now);
        }
        if (request.basicEducation() != null) {
            entity.setBasicEducation(request.basicEducation());
        }
        if (request.inflowType() != null) {
            entity.setInflowType(request.inflowType());
        }
        if (request.applyDate() != null) {
            entity.setApplyDate(request.applyDate());
        }
        if (request.receptionDate() != null) {
            entity.setReceptionDate(request.receptionDate());
        }
        if (request.contactAttempt() != null) {
            entity.setContactAttempt(request.contactAttempt());
        }
        entity.setUpdatedAt(now);
        courseParticipantRepository.save(entity);

        return new CourseParticipantUpdatedResponse(true);
    }

    @Override
    public CourseParticipantDeletedResponse delete(Long courseParticipantId) {
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        courseParticipantCounselorRepository.deleteByCourseParticipantId(courseParticipantId);
        courseParticipantRepository.delete(entity);
        return new CourseParticipantDeletedResponse(true);
    }

    @Override
    public CourseParticipantCanceledResponse cancel(Long courseParticipantId, CancelCourseParticipantRequest request) {
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        entity.setStatus(CourseParticipantStatus.CANCELED);
        entity.setIncompleteReason(request == null ? null : request.reason());
        entity.setUpdatedAt(LocalDateTime.now());
        courseParticipantRepository.save(entity);
        return new CourseParticipantCanceledResponse(entity.getStatus().name());
    }

    @Override
    public CourseParticipantCompletionResponse complete(
            Long courseParticipantId,
            CompleteCourseParticipantRequest request) {
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        CourseParticipantStatus status = parseCompletionStatus(request.status());
        applyCompletion(entity, status, request.completionDate(), request.incompleteReason(), LocalDateTime.now());

        return new CourseParticipantCompletionResponse(entity.getCourseParticipantId(), entity.getStatus().name());
    }

    @Override
    public BulkCompletionResponse bulkComplete(BulkCompleteCourseParticipantRequest request) {
        CourseParticipantStatus status = parseCompletionStatus(request.status());
        LocalDateTime now = LocalDateTime.now();
        // 없는 id 는 findEntity 가 예외를 던져 트랜잭션 전체가 롤백된다(부분 반영 방지).
        List<Long> updatedIds = new ArrayList<>();
        for (Long id : request.courseParticipantIds()) {
            CourseParticipantEntity entity = findEntity(id);
            applyCompletion(entity, status, request.completionDate(), request.incompleteReason(), now);
            updatedIds.add(entity.getCourseParticipantId());
        }
        return new BulkCompletionResponse(updatedIds.size(), updatedIds);
    }

    private void applyCompletion(
            CourseParticipantEntity entity,
            CourseParticipantStatus status,
            java.time.LocalDate completionDate,
            String incompleteReason,
            LocalDateTime now) {
        entity.setStatus(status);
        entity.setCompletionDate(completionDate);
        entity.setIncompleteReason(incompleteReason);
        entity.setUpdatedAt(now);
        courseParticipantRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignableCounselorResponse findAssignableCounselors(Long courseParticipantId) {
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        List<AssignableCounselorResponse.Item> items =
                courseStaffRepository.findByCourseIdAndStaffRole(entity.getCourseId(), StaffRole.COUNSELOR).stream()
                        .map(this::toAssignableItem)
                        .toList();
        return new AssignableCounselorResponse(items);
    }

    private AssignableCounselorResponse.Item toAssignableItem(CourseStaffEntity staff) {
        String name = staff.getUser() == null ? null : staff.getUser().getName();
        return new AssignableCounselorResponse.Item(staff.getUserId(), name);
    }

    @Override
    public CounselorChangedResponse assignSlotCounselor(
            Long courseParticipantId,
            String counselingType,
            AssignSlotCounselorRequest request,
            Long requesterUserId,
            boolean requesterIsCounselorOnly) {
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        CounselingType type = parseCounselingType(counselingType);
        Long targetCounselorId = request.counselorId();

        if (requesterIsCounselorOnly) {
            if (type == CounselingType.PRE_SESSION) {
                if (!isCounselorAssignedToCourse(entity.getCourseId(), requesterUserId)) {
                    throw new BusinessException(ErrorCode.FORBIDDEN_COUNSELOR_ASSIGN);
                }
            } else {
                CounselingType predecessor = predecessorSlot(type);
                boolean assignedToPredecessor = predecessor != null
                        && courseParticipantCounselorRepository
                        .existsByCourseParticipantIdAndCounselorIdAndStatus(
                                courseParticipantId, requesterUserId, predecessor);
                if (!assignedToPredecessor) {
                    throw new BusinessException(ErrorCode.FORBIDDEN_COUNSELOR_ASSIGN);
                }
            }
        }
        validateCounselorAssignable(entity.getCourseId(), targetCounselorId);

        Long oldCounselorId = courseParticipantCounselorRepository
                .findByCourseParticipantIdAndStatus(courseParticipantId, type)
                .map(CourseParticipantCounselorEntity::getCounselorId)
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();
        upsertSlotCounselor(entity, type, targetCounselorId, now);
        recordCounselorChangeHistory(
                entity, type, oldCounselorId, targetCounselorId,
                requesterUserId, request.changedBy(), request.reason(), now);
        return new CounselorChangedResponse(entity.getCourseParticipantId(), counselorSummaries(entity));
    }

    @Override
    public BulkCounselorAssignResponse bulkAssignCounselor(BulkAssignCounselorRequest request) {
        CounselingType type = parseCounselingType(request.counselingType());
        Long targetCounselorId = request.counselorId();
        LocalDateTime now = LocalDateTime.now();
        List<Long> updatedIds = new ArrayList<>();
        for (Long id : request.courseParticipantIds()) {
            CourseParticipantEntity entity = findEntity(id);
            validateCounselorAssignable(entity.getCourseId(), targetCounselorId);
            upsertSlotCounselor(entity, type, targetCounselorId, now);
            updatedIds.add(entity.getCourseParticipantId());
        }
        return new BulkCounselorAssignResponse(updatedIds.size(), updatedIds);
    }

    private void upsertSlotCounselor(
            CourseParticipantEntity entity, CounselingType type, Long targetCounselorId, LocalDateTime now) {
        CourseParticipantCounselorEntity row = courseParticipantCounselorRepository
                .findByCourseParticipantIdAndStatus(entity.getCourseParticipantId(), type)
                .orElse(null);
        if (row == null) {
            row = CourseParticipantCounselorEntity.builder()
                    .courseParticipantId(entity.getCourseParticipantId())
                    .counselorId(targetCounselorId)
                    .status(type)
                    .createdAt(now)
                    .build();
        } else {
            row.setCounselorId(targetCounselorId);
            row.setCounselingStartedAt(null);
            row.setCounselingEndedAt(null);
            row.setCounselingMemo(null);
        }
        courseParticipantCounselorRepository.save(row);

        entity.setUpdatedAt(now);
        courseParticipantRepository.save(entity);
    }

    private void validateCounselorAssignable(Long courseId, Long counselorId) {
        if (!isCounselorAssignedToCourse(courseId, counselorId)) {
            throw new BusinessException(ErrorCode.COUNSELOR_NOT_ASSIGNABLE);
        }
    }

    private boolean isCounselorAssignedToCourse(Long courseId, Long userId) {
        return courseStaffRepository
                .findByCourseIdAndStaffRole(courseId, StaffRole.COUNSELOR).stream()
                .anyMatch(staff -> staff.getUserId().equals(userId));
    }

    private void recordCounselorChangeHistory(
            CourseParticipantEntity entity, CounselingType slot,
            Long oldCounselorId, Long newCounselorId,
            Long actorUserId, ChangeSubject changedBy, String reason, LocalDateTime now) {
        counselorChangeHistoryRepository.save(historyBuilder(entity, slot, actorUserId, changedBy, reason, now)
                .changeType(CounselorChangeType.COUNSELOR_CHANGE)
                .oldCounselorId(oldCounselorId)
                .newCounselorId(newCounselorId)
                .build());
    }

    private void recordScheduleChangeHistory(
            CourseParticipantEntity entity, CounselingType slot, Long counselorId,
            LocalDateTime oldStartedAt, LocalDateTime newStartedAt,
            LocalDateTime oldEndedAt, LocalDateTime newEndedAt,
            Long actorUserId, ChangeSubject changedBy, String reason, LocalDateTime now) {
        counselorChangeHistoryRepository.save(historyBuilder(entity, slot, actorUserId, changedBy, reason, now)
                .changeType(CounselorChangeType.SCHEDULE_CHANGE)
                .oldCounselorId(counselorId)
                .newCounselorId(counselorId)
                .oldStartedAt(oldStartedAt)
                .newStartedAt(newStartedAt)
                .oldEndedAt(oldEndedAt)
                .newEndedAt(newEndedAt)
                .build());
    }

    private CounselorChangeHistoryEntity.CounselorChangeHistoryEntityBuilder historyBuilder(
            CourseParticipantEntity entity, CounselingType slot,
            Long actorUserId, ChangeSubject changedBy, String reason, LocalDateTime now) {
        CourseEntity course = entity.getCourse();
        return CounselorChangeHistoryEntity.builder()
                .accountUserId(actorUserId)
                .courseParticipantId(entity.getCourseParticipantId())
                .courseNumber(course == null ? null : course.getCourseNumber())
                .regionId(course == null ? null : course.getRegionId())
                .counselingType(slot)
                .changedDate(now)
                .changedBy(changedBy)
                .reason(reason)
                .createdAt(now);
    }

    @Override
    @Transactional(readOnly = true)
    public CounselorChangeHistoryResponse getCounselorHistory(Long courseParticipantId) {
        findEntity(courseParticipantId);
        return CounselorChangeHistoryResponse.from(
                counselorChangeHistoryRepository.findByCourseParticipantIdOrderByHistoryIdDesc(courseParticipantId));
    }

    private CounselingType predecessorSlot(CounselingType type) {
        return switch (type) {
            case POST_SESSION_1 -> CounselingType.PRE_SESSION;
            case POST_SESSION_2 -> CounselingType.POST_SESSION_1;
            case PRE_SESSION -> null;
        };
    }

    @Override
    public CourseParticipantStatusChangedResponse changeStatus(
            Long courseParticipantId,
            ChangeCourseParticipantStatusRequest request) {
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        CourseParticipantStatus status = parseRequiredStatus(request.status());

        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        courseParticipantRepository.save(entity);

        return new CourseParticipantStatusChangedResponse(entity.getCourseParticipantId(), entity.getStatus().name());
    }

    @Override
    public ContactAttemptResponse increaseContactAttempt(Long courseParticipantId) {
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        int current = entity.getContactAttempt() == null ? 0 : entity.getContactAttempt();
        entity.setContactAttempt(current + 1);
        entity.setUpdatedAt(LocalDateTime.now());
        courseParticipantRepository.save(entity);
        return new ContactAttemptResponse(entity.getCourseParticipantId(), entity.getContactAttempt());
    }

    @Override
    public CounselorChangedResponse changeCounselor(
            Long courseParticipantId, ChangeCounselorRequest request, Long actorUserId) {
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        LocalDateTime now = LocalDateTime.now();

        Map<CounselingType, Long> oldBySlot = courseParticipantCounselorRepository
                .findByCourseParticipantId(courseParticipantId).stream()
                .collect(Collectors.toMap(
                        CourseParticipantCounselorEntity::getStatus,
                        CourseParticipantCounselorEntity::getCounselorId,
                        (a, b) -> a));

        replaceCounselors(entity.getCourseParticipantId(), request.counselors(), now);
        entity.setUpdatedAt(now);
        courseParticipantRepository.save(entity);

        for (CounselorAssignment assignment : request.counselors()) {
            CounselingType slot = parseCounselingType(assignment.status());
            recordCounselorChangeHistory(
                    entity, slot, oldBySlot.get(slot), assignment.counselorId(),
                    actorUserId, request.changedBy(), request.reason(), now);
        }

        return new CounselorChangedResponse(
                entity.getCourseParticipantId(),
                counselorSummaries(entity));
    }

    @Override
    public CounselingSessionResponse recordCounselingSession(
            Long courseParticipantId,
            String counselingType,
            RecordCounselingSessionRequest request,
            Long requesterUserId,
            boolean requesterIsCounselorOnly) {
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        CounselingType type = parseCounselingType(counselingType);
        CourseParticipantCounselorEntity row = courseParticipantCounselorRepository
                .findByCourseParticipantIdAndStatus(entity.getCourseParticipantId(), type)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUNSELING_SLOT_NOT_FOUND));

        if (requesterIsCounselorOnly && !row.getCounselorId().equals(requesterUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_COUNSELING_RECORD);
        }

        LocalDateTime oldStartedAt = row.getCounselingStartedAt();
        LocalDateTime oldEndedAt = row.getCounselingEndedAt();
        LocalDateTime startedAt = request.startedAt() != null ? request.startedAt() : oldStartedAt;
        LocalDateTime endedAt = request.endedAt() != null ? request.endedAt() : oldEndedAt;
        validateCounselingTime(startedAt, endedAt);

        row.setCounselingStartedAt(startedAt);
        row.setCounselingEndedAt(endedAt);
        if (request.memo() != null) {
            row.setCounselingMemo(request.memo());
        }
        courseParticipantCounselorRepository.save(row);

        boolean scheduleChanged = !Objects.equals(oldStartedAt, startedAt)
                || !Objects.equals(oldEndedAt, endedAt);
        if (scheduleChanged) {
            recordScheduleChangeHistory(
                    entity, type, row.getCounselorId(),
                    oldStartedAt, startedAt, oldEndedAt, endedAt,
                    requesterUserId, request.changedBy(), request.reason(), LocalDateTime.now());
        }

        return new CounselingSessionResponse(
                entity.getCourseParticipantId(),
                type.name(),
                row.getCounselorId(),
                row.getCounselor() == null ? null : row.getCounselor().getName(),
                row.getCounselingStartedAt(),
                row.getCounselingEndedAt(),
                row.getCounselingMemo(),
                row.isCompleted());
    }

    private void validateCounselingTime(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (endedAt == null) {
            return;
        }
        if (startedAt == null || endedAt.isBefore(startedAt)) {
            throw new BusinessException(ErrorCode.INVALID_COUNSELING_TIME);
        }
    }

    private void validateCourseExists(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
    }

    private void validateParticipantExists(Long participantId) {
        if (!participantRepository.existsById(participantId)) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
    }

    private void validateCounselorExists(Long counselorId) {
        if (counselorId == null) {
            return;
        }
        usersRepository.findByUserIdAndDeletedFalse(counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void replaceCounselors(
            Long courseParticipantId, List<CounselorAssignment> assignments, LocalDateTime now) {
        List<CourseParticipantCounselorEntity> rows =
                buildValidatedCounselorRows(courseParticipantId, assignments, now);
        courseParticipantCounselorRepository.deleteByCourseParticipantId(courseParticipantId);
        courseParticipantCounselorRepository.flush();
        if (!rows.isEmpty()) {
            courseParticipantCounselorRepository.saveAll(rows);
        }
    }

    private List<CourseParticipantCounselorEntity> buildValidatedCounselorRows(
            Long courseParticipantId, List<CounselorAssignment> assignments, LocalDateTime now) {
        List<CourseParticipantCounselorEntity> rows = new ArrayList<>();
        if (assignments == null || assignments.isEmpty()) {
            return rows;
        }
        Set<CounselingType> seen = new LinkedHashSet<>();
        for (CounselorAssignment assignment : assignments) {
            Long counselorId = assignment.counselorId();
            CounselingType status = parseCounselingType(assignment.status());
            if (!seen.add(status)) {
                throw new BusinessException(ErrorCode.COUNSELING_SLOT_DUPLICATED);
            }
            validateCounselorExists(counselorId);
            rows.add(CourseParticipantCounselorEntity.builder()
                    .courseParticipantId(courseParticipantId)
                    .counselorId(counselorId)
                    .status(status)
                    .createdAt(now)
                    .build());
        }
        return rows;
    }

    private CounselingType parseCounselingType(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        try {
            return CounselingType.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
    }

    private List<CounselorSummary> counselorSummaries(CourseParticipantEntity cp) {
        return courseParticipantCounselorRepository.findByCourseParticipantId(cp.getCourseParticipantId())
                .stream()
                .map(CounselorSummary::from)
                .toList();
    }

    private CourseParticipantEntity findEntity(Long courseParticipantId) {
        return courseParticipantRepository.findById(courseParticipantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND));
    }

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

    private CourseParticipantStatus parseRequiredStatus(String status) {
        CourseParticipantStatus parsed = parseStatus(status);
        if (parsed == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        return parsed;
    }

    private CourseParticipantStatus parseCompletionStatus(String status) {
        CourseParticipantStatus parsed = parseStatus(status);
        if (parsed != CourseParticipantStatus.COMPLETED && parsed != CourseParticipantStatus.INCOMPLETE) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        return parsed;
    }

    private boolean matchesStatus(CourseParticipantEntity cp, CourseParticipantStatus status) {
        return status == null || cp.getStatus() == status;
    }

    private boolean matchesRegion(CourseParticipantEntity cp, List<Long> regionIds) {
        if (regionIds == null) {
            return true;
        }
        CourseEntity course = cp.getCourse();
        return course != null && regionIds.contains(course.getRegionId());
    }

    private boolean matchesCourseNumber(CourseParticipantEntity cp, Integer courseNumber) {
        if (courseNumber == null) {
            return true;
        }
        CourseEntity course = cp.getCourse();
        return course != null && courseNumber.equals(course.getCourseNumber());
    }

    private boolean matchesLocalCourseNumber(CourseParticipantEntity cp, Integer localCourseNumber) {
        if (localCourseNumber == null) {
            return true;
        }
        CourseEntity course = cp.getCourse();
        return course != null && localCourseNumber.equals(course.getLocalCourseNumber());
    }

    private boolean matchesRegisterDate(
            CourseParticipantEntity cp, java.time.LocalDate from, java.time.LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        LocalDateTime createdAt = cp.getCreatedAt();
        if (createdAt == null) {
            return false;
        }
        java.time.LocalDate date = createdAt.toLocalDate();
        if (from != null && date.isBefore(from)) {
            return false;
        }
        return to == null || !date.isAfter(to);
    }

    private boolean matchesKeyword(CourseParticipantEntity cp, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        ParticipantEntity participant = cp.getParticipant();
        if (participant == null) {
            return false;
        }
        String lowered = keyword.toLowerCase();
        boolean nameMatch = StringUtils.hasText(participant.getName())
                && participant.getName().toLowerCase().contains(lowered);
        boolean phoneMatch = StringUtils.hasText(participant.getPhone())
                && participant.getPhone().toLowerCase().contains(lowered);
        return nameMatch || phoneMatch;
    }

    private CourseParticipantListResponse.Item toListItem(CourseParticipantEntity cp) {
        CourseEntity course = cp.getCourse();
        return new CourseParticipantListResponse.Item(
                cp.getCourseParticipantId(),
                participantName(cp),
                participantMatchKey(cp),
                participantPhone(cp),
                regionName(course),
                courseName(cp),
                course == null ? null : course.getCourseNumber(),
                course == null ? null : course.getLocalCourseNumber(),
                statusName(cp),
                counselorSummaries(cp));
    }

    private String participantMatchKey(CourseParticipantEntity cp) {
        return cp.getParticipant() == null ? null : cp.getParticipant().getMatchKey();
    }

    private String participantName(CourseParticipantEntity cp) {
        return cp.getParticipant() == null ? null : cp.getParticipant().getName();
    }

    private String participantPhone(CourseParticipantEntity cp) {
        return cp.getParticipant() == null ? null : cp.getParticipant().getPhone();
    }

    private String courseName(CourseParticipantEntity cp) {
        return cp.getCourse() == null ? null : cp.getCourse().getCourseName();
    }

    private String statusName(CourseParticipantEntity cp) {
        return cp.getStatus() == null ? null : cp.getStatus().name();
    }

    private Page<CourseParticipantEntity> toPage(List<CourseParticipantEntity> content, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= content.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, content.size());
        }
        int end = Math.min(start + pageable.getPageSize(), content.size());
        return new PageImpl<>(content.subList(start, end), pageable, content.size());
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

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAllIds(
            Long courseId,
            Long regionId,
            Long parentRegionId,
            Integer courseNumber,
            Integer localCourseNumber,
            String status,
            String keyword,
            Set<Long> allowedCourseParticipantIds,
            java.time.LocalDate registerDateFrom,
            java.time.LocalDate registerDateTo) {
        CourseParticipantStatus parsedStatus = parseStatus(status);
        String normalizedKeyword = normalize(keyword);
        List<Long> regionIds = regionResolver.resolveRegionIds(regionId, parentRegionId);
        if (regionIds != null && regionIds.isEmpty()) {
            return List.of();
        }

        List<CourseParticipantEntity> base = courseId == null
                ? courseParticipantRepository.findAll()
                : courseParticipantRepository.findByCourseId(courseId);

        return base.stream()
                .filter(cp -> matchesStatus(cp, parsedStatus))
                .filter(cp -> matchesKeyword(cp, normalizedKeyword))
                .filter(cp -> matchesRegion(cp, regionIds))
                .filter(cp -> matchesCourseNumber(cp, courseNumber))
                .filter(cp -> matchesLocalCourseNumber(cp, localCourseNumber))
                .filter(cp -> matchesRegisterDate(cp, registerDateFrom, registerDateTo))
                .filter(cp -> allowedCourseParticipantIds == null
                        || allowedCourseParticipantIds.contains(cp.getCourseParticipantId()))
                .map(CourseParticipantEntity::getCourseParticipantId)
                .toList();
    }
}
