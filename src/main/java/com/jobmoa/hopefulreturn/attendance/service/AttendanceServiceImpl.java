package com.jobmoa.hopefulreturn.attendance.service;

import com.jobmoa.hopefulreturn.attendance.entity.AttendanceEntity;
import com.jobmoa.hopefulreturn.attendance.entity.AttendanceStatus;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceDeletedResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceDetailResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceListResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceUpdatedResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.BulkAttendanceRequest;
import com.jobmoa.hopefulreturn.attendance.model.dto.BulkAttendanceResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.RegisterAttendanceRequest;
import com.jobmoa.hopefulreturn.attendance.model.dto.UpdateAttendanceRequest;
import com.jobmoa.hopefulreturn.attendance.repository.AttendanceRepository;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
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
public class AttendanceServiceImpl implements AttendanceService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String BULK_SAVED_MESSAGE = "출석 정보가 저장되었습니다.";

    private final AttendanceRepository attendanceRepository;
    private final CourseParticipantRepository courseParticipantRepository;
    private final CourseRepository courseRepository;

    @Override
    public AttendanceResponse register(RegisterAttendanceRequest request) {
        validateCourseParticipantExists(request.courseParticipantId());
        AttendanceStatus status = parseStatus(request.status());

        AttendanceEntity entity = AttendanceEntity.builder()
                .courseParticipantId(request.courseParticipantId())
                .dayNo(request.dayNo())
                .checkInTime(request.checkInTime())
                .checkOutTime(request.checkOutTime())
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();

        AttendanceEntity saved = attendanceRepository.save(entity);
        return new AttendanceResponse(
                saved.getAttendanceId(),
                saved.getCourseParticipantId(),
                saved.getDayNo(),
                saved.getCheckInTime(),
                saved.getCheckOutTime(),
                saved.getStatus() == null ? null : saved.getStatus().name(),
                saved.getCreatedAt());
    }

    @Override
    public BulkAttendanceResponse registerBulk(BulkAttendanceRequest request) {
        if (!courseRepository.existsById(request.courseId())) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        List<AttendanceEntity> entities = new ArrayList<>();
        for (BulkAttendanceRequest.Item item : request.attendances()) {
            validateCourseParticipantExists(item.courseParticipantId());
            entities.add(AttendanceEntity.builder()
                    .courseParticipantId(item.courseParticipantId())
                    .dayNo(request.dayNo())
                    .checkInTime(item.checkInTime())
                    .checkOutTime(item.checkOutTime())
                    .status(parseStatus(item.status()))
                    .createdAt(now)
                    .build());
        }

        List<AttendanceEntity> saved = attendanceRepository.saveAll(entities);
        return new BulkAttendanceResponse(saved.size(), request.dayNo(), request.courseId(), BULK_SAVED_MESSAGE);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceListResponse findAll(Long courseId, Integer dayNo, String status, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(
                sanitizePage(page),
                sanitizeSize(size),
                Sort.by(Sort.Direction.ASC, "attendanceId"));
        AttendanceStatus parsedStatus = parseStatus(status);

        List<AttendanceEntity> filtered = attendanceRepository.findAll().stream()
                .filter(a -> matchesCourse(a, courseId))
                .filter(a -> dayNo == null || dayNo.equals(a.getDayNo()))
                .filter(a -> parsedStatus == null || a.getStatus() == parsedStatus)
                .sorted((a, b) -> Long.compare(a.getAttendanceId(), b.getAttendanceId()))
                .toList();

        Page<AttendanceEntity> pageResult = toPage(filtered, pageable);
        List<AttendanceListResponse.Item> content = pageResult.getContent().stream()
                .map(this::toListItem)
                .toList();

        return new AttendanceListResponse(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceDetailResponse findById(Long attendanceId) {
        AttendanceEntity entity = findEntity(attendanceId);
        return new AttendanceDetailResponse(
                entity.getAttendanceId(),
                entity.getCourseParticipantId(),
                participantName(entity),
                entity.getDayNo(),
                entity.getCheckInTime(),
                entity.getCheckOutTime(),
                statusName(entity),
                entity.getCreatedAt());
    }

    @Override
    public AttendanceUpdatedResponse update(Long attendanceId, UpdateAttendanceRequest request) {
        AttendanceEntity entity = findEntity(attendanceId);
        entity.setStatus(parseStatus(request.status()));
        if (request.checkInTime() != null) {
            entity.setCheckInTime(request.checkInTime());
        }
        if (request.checkOutTime() != null) {
            entity.setCheckOutTime(request.checkOutTime());
        }
        attendanceRepository.save(entity);

        // attendance 테이블에 updated_at 컬럼이 없어 응답 시점 기준 값을 반환한다(미영속).
        return new AttendanceUpdatedResponse(entity.getAttendanceId(), entity.getStatus().name(), LocalDateTime.now());
    }

    @Override
    public AttendanceDeletedResponse delete(Long attendanceId) {
        AttendanceEntity entity = findEntity(attendanceId);
        attendanceRepository.delete(entity);
        return new AttendanceDeletedResponse(true);
    }

    private void validateCourseParticipantExists(Long courseParticipantId) {
        if (!courseParticipantRepository.existsById(courseParticipantId)) {
            throw new BusinessException(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND);
        }
    }

    private AttendanceEntity findEntity(Long attendanceId) {
        return attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTENDANCE_NOT_FOUND));
    }

    private AttendanceStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return AttendanceStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
    }

    private boolean matchesCourse(AttendanceEntity attendance, Long courseId) {
        if (courseId == null) {
            return true;
        }
        CourseParticipantEntity cp = attendance.getCourseParticipant();
        return cp != null && courseId.equals(cp.getCourseId());
    }

    private AttendanceListResponse.Item toListItem(AttendanceEntity attendance) {
        return new AttendanceListResponse.Item(
                attendance.getAttendanceId(),
                participantName(attendance),
                attendance.getDayNo(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                statusName(attendance));
    }

    private String participantName(AttendanceEntity attendance) {
        CourseParticipantEntity cp = attendance.getCourseParticipant();
        if (cp == null || cp.getParticipant() == null) {
            return null;
        }
        return cp.getParticipant().getName();
    }

    private String statusName(AttendanceEntity attendance) {
        return attendance.getStatus() == null ? null : attendance.getStatus().name();
    }

    private Page<AttendanceEntity> toPage(List<AttendanceEntity> content, Pageable pageable) {
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
