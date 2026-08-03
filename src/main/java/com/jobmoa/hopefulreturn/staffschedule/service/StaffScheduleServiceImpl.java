package com.jobmoa.hopefulreturn.staffschedule.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.staffschedule.entity.StaffScheduleEntity;
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
import java.util.Collections;
import java.util.List;
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
public class StaffScheduleServiceImpl implements StaffScheduleService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final StaffScheduleRepository staffScheduleRepository;
    private final UsersRepository usersRepository;

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

        List<StaffScheduleEntity> filtered =
                staffScheduleRepository.findByUserIdAndScheduleDateBetween(requesterId, from, to).stream()
                        .sorted((a, b) -> Long.compare(a.getStaffScheduleId(), b.getStaffScheduleId()))
                        .toList();

        // /me 는 페이지네이션 없이 전체 반환하되 목록 응답 포맷을 재사용한다
        Pageable pageable = PageRequest.of(DEFAULT_PAGE, Math.max(filtered.size(), 1),
                Sort.by(Sort.Direction.ASC, "staffScheduleId"));
        return toListResponse(filtered, pageable);
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

        if (StringUtils.hasText(request.sessionType())) {
            entity.setSessionType(parseSessionType(request.sessionType()));
        }
        if (request.isAvailable() != null) {
            entity.setIsAvailable(request.isAvailable());
        }
        if (request.memo() != null) {
            entity.setMemo(request.memo());
        }
        entity.setUpdatedAt(LocalDateTime.now());

        return toResponse(staffScheduleRepository.save(entity));
    }

    @Override
    public StaffScheduleDeletedResponse delete(Long staffScheduleId, Long requesterId, boolean isManager) {
        StaffScheduleEntity entity = findEntity(staffScheduleId);
        assertOwnerOrManager(entity.getUserId(), requesterId, isManager);
        staffScheduleRepository.delete(entity);
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
        List<StaffScheduleListResponse.Item> items = pageResult.getContent().stream()
                .map(this::toListItem)
                .toList();
        return new StaffScheduleListResponse(
                items,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages());
    }

    private StaffScheduleListResponse.Item toListItem(StaffScheduleEntity entity) {
        return new StaffScheduleListResponse.Item(
                entity.getStaffScheduleId(),
                entity.getUserId(),
                userName(entity),
                entity.getScheduleDate(),
                entity.getSessionType() == null ? null : entity.getSessionType().name(),
                entity.getIsAvailable(),
                entity.getMemo());
    }

    private StaffScheduleResponse toResponse(StaffScheduleEntity entity) {
        return new StaffScheduleResponse(
                entity.getStaffScheduleId(),
                entity.getUserId(),
                userName(entity),
                entity.getScheduleDate(),
                entity.getSessionType() == null ? null : entity.getSessionType().name(),
                entity.getIsAvailable(),
                entity.getMemo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private String userName(StaffScheduleEntity entity) {
        UsersEntity user = entity.getUser();
        return user == null ? null : user.getName();
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
