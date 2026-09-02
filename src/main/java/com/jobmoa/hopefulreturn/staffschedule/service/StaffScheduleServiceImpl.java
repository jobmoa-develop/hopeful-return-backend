package com.jobmoa.hopefulreturn.staffschedule.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.coursedailycounselor.entity.CourseDailyCounselorEntity;
import com.jobmoa.hopefulreturn.coursedailycounselor.repository.CourseDailyCounselorRepository;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.staffschedule.entity.StaffScheduleEntity;
import com.jobmoa.hopefulreturn.staffschedule.event.StaffBecameUnavailableEvent;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.BulkStaffScheduleRequest;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.BulkStaffScheduleResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.CreateStaffScheduleRequest;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.StaffScheduleDeletedResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.StaffScheduleListResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.StaffScheduleResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.UpdateStaffScheduleRequest;
import com.jobmoa.hopefulreturn.staffschedule.repository.StaffScheduleRepository;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
public class StaffScheduleServiceImpl implements StaffScheduleService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final StaffScheduleRepository staffScheduleRepository;
    private final UsersRepository usersRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CourseDailyCounselorRepository courseDailyCounselorRepository;
    private final CourseStaffRepository courseStaffRepository;

    @Override
    public StaffScheduleResponse create(Long requesterId, boolean isManager, CreateStaffScheduleRequest request) {
        // 대상 스태프 결정: userId 생략 시 본인, 지정 시 타인 → 관리자만 허용
        Long targetUserId = resolveTargetUserId(request.userId(), requesterId, isManager);
        validateUserExists(targetUserId);
        SessionType sessionType = parseSessionType(request.sessionType());
        validateNotDuplicated(targetUserId, request.scheduleDate(), sessionType);

        StaffScheduleEntity entity = StaffScheduleEntity.builder()
                .userId(targetUserId)
                .scheduleDate(request.scheduleDate())
                .sessionType(sessionType)
                .isAvailable(request.isAvailable() == null ? Boolean.TRUE : request.isAvailable())
                .memo(request.memo())
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(staffScheduleRepository.save(entity));
    }

    @Override
    public BulkStaffScheduleResponse createBulk(
            Long requesterId, boolean isManager, BulkStaffScheduleRequest request) {
        Long targetUserId = resolveTargetUserId(request.userId(), requesterId, isManager);
        validateUserExists(targetUserId);

        LocalDateTime now = LocalDateTime.now();
        List<StaffScheduleEntity> toSave = new ArrayList<>();
        int skipped = 0;
        for (BulkStaffScheduleRequest.Entry entry : request.entries()) {
            SessionType sessionType = parseSessionType(entry.sessionType());
            // UNIQUE(user·date·session) 충돌은 스킵(무시)한다
            if (staffScheduleRepository.existsByUserIdAndScheduleDateAndSessionType(
                    targetUserId, entry.scheduleDate(), sessionType)) {
                skipped++;
                continue;
            }
            toSave.add(StaffScheduleEntity.builder()
                    .userId(targetUserId)
                    .scheduleDate(entry.scheduleDate())
                    .sessionType(sessionType)
                    .isAvailable(entry.isAvailable() == null ? Boolean.TRUE : entry.isAvailable())
                    .memo(entry.memo())
                    .createdAt(now)
                    .build());
        }

        int registered = toSave.isEmpty() ? 0 : staffScheduleRepository.saveAll(toSave).size();
        String message = String.format("%d건 등록, %d건 스킵되었습니다.", registered, skipped);
        return new BulkStaffScheduleResponse(registered, skipped, message);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffScheduleListResponse findAll(
            Long userId, LocalDate fromDate, LocalDate toDate, String sessionType, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(
                sanitizePage(page), sanitizeSize(size), Sort.by(Sort.Direction.ASC, "staffScheduleId"));
        SessionType parsedSessionType = StringUtils.hasText(sessionType) ? parseSessionType(sessionType) : null;

        List<StaffScheduleEntity> filtered = staffScheduleRepository.findAll().stream()
                .filter(s -> userId == null || userId.equals(s.getUserId()))
                .filter(s -> fromDate == null || !s.getScheduleDate().isBefore(fromDate))
                .filter(s -> toDate == null || !s.getScheduleDate().isAfter(toDate))
                .filter(s -> parsedSessionType == null || s.getSessionType() == parsedSessionType)
                .sorted((a, b) -> Long.compare(a.getStaffScheduleId(), b.getStaffScheduleId()))
                .toList();

        return toListResponse(filtered, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffScheduleListResponse findMy(Long requesterId, LocalDate fromDate, LocalDate toDate) {
        // 범위 미지정 시 전체 조회(초광범위 방지용 하한/상한)
        LocalDate from = fromDate == null ? LocalDate.of(1900, 1, 1) : fromDate;
        LocalDate to = toDate == null ? LocalDate.of(9999, 12, 31) : toDate;

        // 1) 본인 staff_schedule 행(가용/불가 + 비-상담사 배정)
        List<StaffScheduleEntity> staffRows =
                staffScheduleRepository.findByUserIdAndScheduleDateBetween(requesterId, from, to).stream()
                        .sorted((a, b) -> Long.compare(a.getStaffScheduleId(), b.getStaffScheduleId()))
                        .toList();

        // 2) 본인 상담사 회차 배정(course_daily_counselor)을 읽기 전용 배정행으로 합성해 노출한다.
        //    상담사 배정은 staff_schedule 을 쓰지 않으므로(같은 날 다중 회차 허용) 개인 캘린더에서
        //    보이도록 별도 병합한다. staffScheduleId=null(합성행) → FE 에서 읽기 전용으로 렌더.
        List<CourseDailyCounselorEntity> counselorRows =
                courseDailyCounselorRepository.findByUserIdAndScheduleDateBetween(requesterId, from, to);

        // 배정 회차명·상태는 양쪽 행의 courseStaffId 합집합을 한 번에 조회해 채운다(N+1 방지).
        List<Long> courseStaffIds = new ArrayList<>();
        staffRows.forEach(s -> courseStaffIds.add(s.getCourseStaffId()));
        counselorRows.forEach(cdc -> {
            CourseStaffEntity cs = cdc.getCourseStaff();
            courseStaffIds.add(cs == null ? null : cs.getCourseStaffId());
        });
        Map<Long, CourseRef> courseRefs = resolveCourseRefs(courseStaffIds);

        List<StaffScheduleListResponse.Item> items = new ArrayList<>(
                staffRows.stream().map(e -> toListItem(e, courseRefs)).toList());

        for (CourseDailyCounselorEntity cdc : counselorRows) {
            CourseStaffEntity cs = cdc.getCourseStaff();
            String name = cs == null || cs.getUser() == null ? null : cs.getUser().getName();
            Long courseStaffId = cs == null ? null : cs.getCourseStaffId();
            CourseRef ref = courseRefs.get(courseStaffId);
            items.add(new StaffScheduleListResponse.Item(
                    null, requesterId, name, cdc.getScheduleDate(),
                    SessionType.FULL.name(), Boolean.TRUE,
                    courseStaffId, ref == null ? null : ref.courseId(),
                    ref == null ? null : ref.name(), ref == null ? null : ref.status(),
                    ref == null ? null : ref.role(), null));
        }

        // /me 는 페이지네이션 없이 전체 반환한다(목록 응답 포맷 재사용).
        return new StaffScheduleListResponse(items, DEFAULT_PAGE, items.size(), items.size(), 1);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffScheduleResponse findById(Long staffScheduleId) {
        return toResponse(findEntity(staffScheduleId));
    }

    @Override
    public StaffScheduleResponse update(
            Long staffScheduleId, Long requesterId, boolean isManager, UpdateStaffScheduleRequest request) {
        StaffScheduleEntity entity = findEntity(staffScheduleId);
        assertOwnerOrManager(entity.getUserId(), requesterId, isManager);

        // 알림 판정·배정 해제를 위해 변경 전 상태를 세팅 전에 캡처한다.
        boolean wasAvailable = Boolean.TRUE.equals(entity.getIsAvailable());
        Long assignedCourseStaffId = entity.getCourseStaffId();

        if (StringUtils.hasText(request.sessionType())) {
            entity.setSessionType(parseSessionType(request.sessionType()));
        }
        if (request.isAvailable() != null) {
            entity.setIsAvailable(request.isAvailable());
        }
        if (request.memo() != null) {
            entity.setMemo(request.memo());
        }

        // 배정된 날짜(course_staff_id 有)를 가능→불가로 바꾸면 삭제와 동일하게 인력배정에서 제외한다.
        // course_staff_id 연결만 해제하고 행은 불가 표식(is_available=false)으로 남겨, 그 날짜가
        // 후보 제외에 반영되도록 한다.
        boolean becameUnavailable = wasAvailable && Boolean.FALSE.equals(request.isAvailable());
        boolean droppedFromAssignment = becameUnavailable && assignedCourseStaffId != null;
        if (droppedFromAssignment) {
            entity.setCourseStaffId(null);
        }
        entity.setUpdatedAt(LocalDateTime.now());

        StaffScheduleEntity saved = staffScheduleRepository.save(entity);
        // 배정 해제로 엔티티의 courseStaffId 는 이미 null 이므로, 알림은 캡처한 원래 배정 ID 로 발행한다.
        // (배정된 날짜를 불가로 바꾼 경우에만 관리자 재배정 알림, 순수 근무불가일은 제외.)
        if (droppedFromAssignment) {
            publishReassignmentNotice(saved, assignedCourseStaffId, saved.getMemo());
        }
        return toResponse(saved);
    }

    /** 배정된 날짜가 불가로 바뀌거나 삭제될 때 관리자 재배정 알림 이벤트를 발행한다(사유=memo). */
    private void publishReassignmentNotice(StaffScheduleEntity entity, Long courseStaffId, String memo) {
        eventPublisher.publishEvent(new StaffBecameUnavailableEvent(
                entity.getStaffScheduleId(),
                courseStaffId,
                entity.getUserId(),
                entity.getScheduleDate(),
                entity.getSessionType(),
                memo));
    }

    @Override
    public StaffScheduleDeletedResponse delete(
            Long staffScheduleId, Long requesterId, boolean isManager, String reason) {
        StaffScheduleEntity entity = findEntity(staffScheduleId);
        assertOwnerOrManager(entity.getUserId(), requesterId, isManager);
        // 배정된 날짜(course_staff_id NOT NULL) 삭제면 불가 전환과 동일하게 재배정 알림을 보낸다(사유=reason).
        Long courseStaffId = entity.getCourseStaffId();
        boolean assigned = courseStaffId != null;
        staffScheduleRepository.delete(entity);
        if (assigned) {
            publishReassignmentNotice(entity, courseStaffId, reason);
        }
        return new StaffScheduleDeletedResponse(true);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Long resolveTargetUserId(Long requestUserId, Long requesterId, boolean isManager) {
        if (requestUserId == null || requestUserId.equals(requesterId)) {
            return requesterId;
        }
        // 타인 등록은 관리자(ADMIN·OPERATOR)만 허용
        if (!isManager) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return requestUserId;
    }

    private void assertOwnerOrManager(Long rowUserId, Long requesterId, boolean isManager) {
        if (!isManager && !rowUserId.equals(requesterId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateUserExists(Long userId) {
        if (usersRepository.findByUserIdAndDeletedFalse(userId).isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateNotDuplicated(Long userId, LocalDate scheduleDate, SessionType sessionType) {
        if (staffScheduleRepository.existsByUserIdAndScheduleDateAndSessionType(userId, scheduleDate, sessionType)) {
            throw new BusinessException(ErrorCode.DUPLICATE_STAFF_SCHEDULE);
        }
    }

    private StaffScheduleEntity findEntity(Long staffScheduleId) {
        return staffScheduleRepository.findById(staffScheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STAFF_SCHEDULE_NOT_FOUND));
    }

    private SessionType parseSessionType(String value) {
        try {
            return SessionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_SESSION_TYPE);
        }
    }

    private StaffScheduleListResponse toListResponse(List<StaffScheduleEntity> content, Pageable pageable) {
        Page<StaffScheduleEntity> pageResult = toPage(content, pageable);
        Map<Long, CourseRef> courseRefs = resolveCourseRefs(
                pageResult.getContent().stream().map(StaffScheduleEntity::getCourseStaffId).toList());
        List<StaffScheduleListResponse.Item> items = pageResult.getContent().stream()
                .map(e -> toListItem(e, courseRefs))
                .toList();
        return new StaffScheduleListResponse(
                items,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages());
    }

    private StaffScheduleListResponse.Item toListItem(StaffScheduleEntity entity, Map<Long, CourseRef> courseRefs) {
        CourseRef ref = courseRefs.get(entity.getCourseStaffId());
        return new StaffScheduleListResponse.Item(
                entity.getStaffScheduleId(),
                entity.getUserId(),
                userName(entity),
                entity.getScheduleDate(),
                entity.getSessionType() == null ? null : entity.getSessionType().name(),
                entity.getIsAvailable(),
                entity.getCourseStaffId(),
                ref == null ? null : ref.courseId(),
                ref == null ? null : ref.name(),
                ref == null ? null : ref.status(),
                ref == null ? null : ref.role(),
                entity.getMemo());
    }

    private StaffScheduleResponse toResponse(StaffScheduleEntity entity) {
        CourseRef ref = resolveCourseRefs(Collections.singletonList(entity.getCourseStaffId()))
                .get(entity.getCourseStaffId());
        return new StaffScheduleResponse(
                entity.getStaffScheduleId(),
                entity.getUserId(),
                userName(entity),
                entity.getScheduleDate(),
                entity.getSessionType() == null ? null : entity.getSessionType().name(),
                entity.getIsAvailable(),
                entity.getCourseStaffId(),
                ref == null ? null : ref.courseId(),
                ref == null ? null : ref.name(),
                ref == null ? null : ref.status(),
                ref == null ? null : ref.role(),
                entity.getMemo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private String userName(StaffScheduleEntity entity) {
        UsersEntity user = entity.getUser();
        return user == null ? null : user.getName();
    }

    /**
     * 배정 회차의 표시 정보(회차명 + 상태 + 배정 역할). 목록·상세 응답의
     * courseName·courseStatus·courseStaffRole 을 함께 채운다.
     */
    private record CourseRef(Long courseId, String name, String status, String role) {
    }

    /**
     * 배정 ID(course_staff) 묶음 → 회차 표시정보(회차명·상태·역할) 매핑. null·중복은 걸러 1회 조회로 해결한다.
     * 회차명 규칙은 알림 서비스와 동일하게 지역회차(localCourseNumber) 우선, 없으면 전체회차(courseNumber).
     * 상태는 CourseEntity.status(enum), 역할은 CourseStaff.staffRole(enum) 을 문자열로 노출한다(FE 배지용).
     * course·region 은 fetch join 으로, staffRole 은 CourseStaff 자체 컬럼이라 추가 쿼리 없이 함께 로드된다.
     */
    private Map<Long, CourseRef> resolveCourseRefs(Collection<Long> courseStaffIds) {
        List<Long> ids = courseStaffIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            // 미배정 행은 get(null) 로 조회되므로, null 키를 허용하는 빈 맵을 반환한다(Map.of()는 NPE).
            return Collections.emptyMap();
        }
        Map<Long, CourseRef> refs = new HashMap<>();
        for (CourseStaffEntity cs : courseStaffRepository.findWithCourseAndRegionByIdIn(ids)) {
            CourseEntity course = cs.getCourse();
            Long courseId = cs.getCourseId();
            String name = courseDisplayName(course);
            String status = course == null || course.getStatus() == null ? null : course.getStatus().name();
            String role = cs.getStaffRole() == null ? null : cs.getStaffRole().name();
            if (courseId != null || name != null || status != null || role != null) {
                refs.put(cs.getCourseStaffId(), new CourseRef(courseId, name, status, role));
            }
        }
        return refs;
    }

    private String courseDisplayName(CourseEntity course) {
        if (course == null) {
            return null;
        }
        String regionName = course.getRegion() == null ? null : course.getRegion().getName();
        Integer round = course.getLocalCourseNumber() != null
                ? course.getLocalCourseNumber()
                : course.getCourseNumber();
        if (regionName == null && round == null) {
            return null;
        }
        if (regionName == null) {
            return round + "회차";
        }
        return round == null ? regionName : regionName + " " + round + "회차";
    }

    private Page<StaffScheduleEntity> toPage(List<StaffScheduleEntity> content, Pageable pageable) {
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
}
