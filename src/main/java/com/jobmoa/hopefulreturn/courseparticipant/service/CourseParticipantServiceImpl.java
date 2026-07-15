package com.jobmoa.hopefulreturn.courseparticipant.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselingType;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CancelCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ChangeCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CompleteCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ContactAttemptResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselingSessionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorAssignment;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorChangedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorSummary;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCanceledResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCompletionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantDetailResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantUpdatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CreateCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.RecordCounselingSessionRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.UpdateCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participant.repository.ParticipantRepository;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final CourseRepository courseRepository;
    private final ParticipantRepository participantRepository;
    private final UsersRepository usersRepository;

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
            String status,
            String keyword,
            Integer page,
            Integer size) {
        Pageable pageable = PageRequest.of(
                sanitizePage(page),
                sanitizeSize(size),
                Sort.by(Sort.Direction.ASC, "courseParticipantId"));
        CourseParticipantStatus parsedStatus = parseStatus(status);
        String normalizedKeyword = normalize(keyword);

        List<CourseParticipantEntity> base = courseId == null
                ? courseParticipantRepository.findAll()
                : courseParticipantRepository.findByCourseId(courseId);
        List<CourseParticipantEntity> filtered = base.stream()
                .filter(cp -> matchesStatus(cp, parsedStatus))
                .filter(cp -> matchesKeyword(cp, normalizedKeyword))
                .sorted((a, b) -> Long.compare(a.getCourseParticipantId(), b.getCourseParticipantId()))
                .toList();

        Page<CourseParticipantEntity> pageResult = toPage(filtered, pageable);
        List<CourseParticipantListResponse.Item> content = pageResult.getContent().stream()
                .map(this::toListItem)
                .toList();

        return new CourseParticipantListResponse(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseParticipantDetailResponse findById(Long courseParticipantId) {
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

        entity.setStatus(status);
        entity.setCompletionDate(request.completionDate());
        entity.setIncompleteReason(request.incompleteReason());
        entity.setUpdatedAt(LocalDateTime.now());
        courseParticipantRepository.save(entity);

        return new CourseParticipantCompletionResponse(entity.getCourseParticipantId(), entity.getStatus().name());
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
    public CounselorChangedResponse changeCounselor(Long courseParticipantId, ChangeCounselorRequest request) {
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        LocalDateTime now = LocalDateTime.now();
        replaceCounselors(entity.getCourseParticipantId(), request.counselors(), now);
        entity.setUpdatedAt(now);
        courseParticipantRepository.save(entity);
        return new CounselorChangedResponse(
                entity.getCourseParticipantId(),
                counselorSummaries(entity));
    }

    @Override
    public CounselingSessionResponse recordCounselingSession(
            Long courseParticipantId, String counselingType, RecordCounselingSessionRequest request) {
        CourseParticipantEntity entity = findEntity(courseParticipantId);
        CounselingType type = parseCounselingType(counselingType);
        CourseParticipantCounselorEntity row = courseParticipantCounselorRepository
                .findByCourseParticipantIdAndStatus(entity.getCourseParticipantId(), type)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUNSELING_SLOT_NOT_FOUND));

        // null 필드는 기존값 유지(부분 수정) — 병합 결과를 기준으로 시간 순서를 검증한다.
        LocalDateTime startedAt = request.startedAt() != null ? request.startedAt() : row.getCounselingStartedAt();
        LocalDateTime endedAt = request.endedAt() != null ? request.endedAt() : row.getCounselingEndedAt();
        validateCounselingTime(startedAt, endedAt);

        row.setCounselingStartedAt(startedAt);
        row.setCounselingEndedAt(endedAt);
        if (request.memo() != null) {
            row.setCounselingMemo(request.memo());
        }
        courseParticipantCounselorRepository.save(row);

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

    /**
     * 수강건의 상담사 배정을 전체 교체한다(하드 삭제 후 재삽입). 검증은 삭제 전에 수행한다.
     */
    private void replaceCounselors(
            Long courseParticipantId, List<CounselorAssignment> assignments, LocalDateTime now) {
        List<CourseParticipantCounselorEntity> rows =
                buildValidatedCounselorRows(courseParticipantId, assignments, now);
        courseParticipantCounselorRepository.deleteByCourseParticipantId(courseParticipantId);
        // 삭제를 재삽입보다 먼저 DB에 반영한다. flush가 없으면 Hibernate의 액션 순서상
        // INSERT가 DELETE보다 먼저 실행돼, 같은 (수강건·상담 구분)을 재배정할 때
        // UQ_CPC_PARTICIPANT_STATUS 유니크 제약에 걸린다.
        courseParticipantCounselorRepository.flush();
        if (!rows.isEmpty()) {
            courseParticipantCounselorRepository.saveAll(rows);
        }
    }

    /**
     * 배정 목록을 검증(상담사 존재·상태값·슬롯 중복)한 저장용 엔티티 목록을 만든다.
     * 슬롯(상담 구분)당 상담사는 1명 — 같은 슬롯이 두 번 오면 예외를 던진다.
     * courseParticipantId가 null이면 저장 직전에 호출자가 채운다.
     */
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
        return new CourseParticipantListResponse.Item(
                cp.getCourseParticipantId(),
                participantName(cp),
                participantPhone(cp),
                statusName(cp),
                counselorSummaries(cp));
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
}
