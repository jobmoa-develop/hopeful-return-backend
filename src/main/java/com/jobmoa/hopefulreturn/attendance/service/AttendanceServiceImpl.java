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
import com.jobmoa.hopefulreturn.attendanceleave.entity.AttendanceLeaveEntity;
import com.jobmoa.hopefulreturn.attendanceleave.repository.AttendanceLeaveRepository;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.attendance.model.dto.CompletionRiskItemResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.CompletionRiskListResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.CompletionRiskStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String BULK_SAVED_MESSAGE = "출석 정보가 저장되었습니다.";

    private final AttendanceRepository attendanceRepository;
    private final AttendanceLeaveRepository attendanceLeaveRepository;
    private final CourseParticipantRepository courseParticipantRepository;
    private final CourseRepository courseRepository;
    private final CourseStaffRepository courseStaffRepository;

    @Override
    public AttendanceResponse register(RegisterAttendanceRequest request) {
        validateCourseParticipantExists(request.courseParticipantId());
        AttendanceStatus status = calculateAttendanceStatus(
                request.courseParticipantId(), request.checkInTime());

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
                    .status(
                            calculateAttendanceStatus(
                                    item.courseParticipantId(),
                                    item.checkInTime()
                            )
                    )
                    .createdAt(now)
                    .build());
        }

        List<AttendanceEntity> saved = attendanceRepository.saveAll(entities);
        return new BulkAttendanceResponse(saved.size(), request.dayNo(), request.courseId(), BULK_SAVED_MESSAGE);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceListResponse findAll(
            Long courseId,
            Long courseParticipantId,
            Integer dayNo,
            String status,
            Long staffScopeId,
            Integer page,
            Integer size) {
        final Set<Long> allowedCourseIds =
                staffScopeId == null
                        ? null
                        : courseStaffRepository.findByUserId(staffScopeId)
                        .stream()
                        .map(cs -> cs.getCourseId())
                        .collect(Collectors.toSet());
        Pageable pageable = PageRequest.of(
                sanitizePage(page),
                sanitizeSize(size),
                Sort.by(Sort.Direction.ASC, "attendanceId"));

        AttendanceStatus parsedStatus = parseStatus(status);

        List<AttendanceEntity> filtered = attendanceRepository.findAll().stream()
                .filter(a -> allowedCourseIds == null
                        || allowedCourseIds.contains(
                        a.getCourseParticipant().getCourseId()))
                .filter(a -> matchesCourse(a, courseId))
                .filter(a -> courseParticipantId == null || courseParticipantId.equals(a.getCourseParticipantId()))
                .filter(a -> dayNo == null || dayNo.equals(a.getDayNo()))
                .filter(a -> parsedStatus == null || a.getStatus() == parsedStatus)
                .sorted((a, b) -> Long.compare(a.getAttendanceId(), b.getAttendanceId()))
                .toList();

        Page<AttendanceEntity> pageResult = toPage(filtered, pageable);
        Map<Long, List<AttendanceListResponse.LeaveItem>> leavesByAttendanceId = findLeaves(pageResult.getContent());

        List<AttendanceListResponse.Item> content = pageResult.getContent().stream()
                .map(a -> toListItem(a, leavesByAttendanceId.getOrDefault(a.getAttendanceId(), List.of())))
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
        List<AttendanceListResponse.LeaveItem> leaves =
                findLeaves(List.of(entity)).getOrDefault(entity.getAttendanceId(), List.of());
        return new AttendanceDetailResponse(
                entity.getAttendanceId(),
                entity.getCourseParticipantId(),
                participantName(entity),
                entity.getDayNo(),
                entity.getCheckInTime(),
                entity.getCheckOutTime(),
                statusName(entity),
                entity.getCreatedAt(),
                leaves);
    }

    @Override
    public AttendanceUpdatedResponse update(Long attendanceId, UpdateAttendanceRequest request) {
        AttendanceEntity entity = findEntity(attendanceId);

        if (request.checkInTime() != null) {
            entity.setCheckInTime(request.checkInTime());
            entity.setStatus(
                    calculateAttendanceStatus(
                            entity.getCourseParticipantId(),
                            request.checkInTime()
                    )
            );
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

    @Override
    @Transactional(readOnly = true)
    public CompletionRiskListResponse findCompletionRisk(Long courseId) {
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        List<CourseParticipantEntity> participants = courseParticipantRepository.findWithParticipantByCourseId(courseId);
        if (participants.isEmpty()) {
            return new CompletionRiskListResponse(List.of());
        }

        List<Long> participantIds = participants.stream()
                .map(CourseParticipantEntity::getCourseParticipantId)
                .toList();

        List<AttendanceEntity> attendances = attendanceRepository.findByCourseParticipantIdIn(participantIds);
        Map<Long, List<AttendanceEntity>> attendanceMap = attendances.stream()
                .collect(Collectors.groupingBy(AttendanceEntity::getCourseParticipantId));

        List<Long> attendanceIds = attendances.stream()
                .map(AttendanceEntity::getAttendanceId)
                .toList();

        Map<Long, List<AttendanceLeaveEntity>> leaveMap = attendanceLeaveRepository.findByAttendanceIdIn(attendanceIds)
                .stream()
                .collect(Collectors.groupingBy(AttendanceLeaveEntity::getAttendanceId));

        List<CompletionRiskItemResponse> items = new ArrayList<>();
        for (CourseParticipantEntity participant : participants) {
            long attendedMinutes = calculateAttendedMinutes(
                    attendanceMap.getOrDefault(participant.getCourseParticipantId(), List.of()),
                    leaveMap);

            long requiredMinutes = calculateRequiredMinutes(course);
            double attendanceRate = requiredMinutes == 0 ? 0 : attendedMinutes * 100.0 / requiredMinutes;

            CompletionRiskStatus risk = calculateRiskStatus(
                    attendedMinutes,
                    requiredMinutes,
                    course,
                    attendanceMap.getOrDefault(participant.getCourseParticipantId(), List.of())
            );

            items.add(new CompletionRiskItemResponse(
                    participant.getCourseParticipantId(),
                    participant.getParticipant().getName(),
                    attendedMinutes,
                    requiredMinutes,
                    attendanceRate,
                    risk
            ));
        }

        return new CompletionRiskListResponse(items);
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

    private Map<Long, List<AttendanceListResponse.LeaveItem>> findLeaves(List<AttendanceEntity> attendances) {
        List<Long> attendanceIds = attendances.stream()
                .map(AttendanceEntity::getAttendanceId)
                .toList();
        if (attendanceIds.isEmpty()) {
            return Map.of();
        }

        return attendanceLeaveRepository.findByAttendanceIdIn(attendanceIds).stream()
                .collect(Collectors.groupingBy(
                        AttendanceLeaveEntity::getAttendanceId,
                        Collectors.mapping(
                                leave -> new AttendanceListResponse.LeaveItem(
                                        leave.getAttendanceLeaveId(),
                                        leave.getLeaveTime(),
                                        leave.getReturnTime(),
                                        leave.getReason()),
                                Collectors.toList())));
    }

    private AttendanceListResponse.Item toListItem(
            AttendanceEntity attendance, List<AttendanceListResponse.LeaveItem> leaves) {
        return new AttendanceListResponse.Item(
                attendance.getAttendanceId(),
                attendance.getCourseParticipantId(),
                participantName(attendance),
                attendance.getDayNo(),
                attendance.getCheckInTime(),
                attendance.getCheckOutTime(),
                statusName(attendance),
                leaves);
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

    private AttendanceStatus calculateAttendanceStatus(
            Long courseParticipantId, LocalTime checkInTime) {
        CourseParticipantEntity participant = courseParticipantRepository.findById(courseParticipantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND));

        CourseEntity course = courseRepository.findById(participant.getCourseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        LocalTime educationStartTime = course.getEducationStartTime();

        // 교육 시작 시간이 등록되지 않은 경우
        if (educationStartTime == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 입실 기록이 없으면 결석
        if (checkInTime == null) {
            return AttendanceStatus.ABSENT;
        }

        // 출석 인정 기준 : 시작시간 + 59초
        LocalTime attendLimit = educationStartTime.plusSeconds(59);

        // 09:00:59 까지 출석
        if (!checkInTime.isAfter(attendLimit)) {
            return AttendanceStatus.ATTEND;
        }

        // 교육 종료 시간
        LocalTime educationEndTime = course.getEducationEndTime();
        if (educationEndTime == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 수업 종료 전까지는 지각
        if (checkInTime.isBefore(educationEndTime)) {
            return AttendanceStatus.LATE;
        }

        // 종료 시각 이후(18:00 포함)는 결석
        return AttendanceStatus.ABSENT;
    }

    private long calculateAttendedMinutes(
            List<AttendanceEntity> attendances, Map<Long, List<AttendanceLeaveEntity>> leaveMap) {
        long totalMinutes = 0;
        for (AttendanceEntity attendance : attendances) {
            if (attendance.getCheckInTime() == null || attendance.getCheckOutTime() == null) {
                continue;
            }

            long minutes = Duration.between(
                            attendance.getCheckInTime(),
                            attendance.getCheckOutTime())
                    .toMinutes();

            List<AttendanceLeaveEntity> leaves = leaveMap.getOrDefault(
                    attendance.getAttendanceId(),
                    List.of());

            for (AttendanceLeaveEntity leave : leaves) {
                if (leave.getLeaveTime() == null || leave.getReturnTime() == null) {
                    continue;
                }
                minutes -= Duration.between(
                                leave.getLeaveTime(),
                                leave.getReturnTime())
                        .toMinutes();
            }

            if (minutes > 0) {
                totalMinutes += minutes;
            }
        }
        return totalMinutes;
    }

    private long calculateRequiredMinutes(CourseEntity course) {
        if (course.getEducationStartTime() == null || course.getEducationEndTime() == null) {
            return 0;
        }

        long minutesPerDay = minutesPerDay(course);
        int educationDays = 0;

        if (course.getDay1Date() != null) educationDays++;
        if (course.getDay2Date() != null) educationDays++;
        if (course.getDay3Date() != null) educationDays++;
        if (course.getDay4Date() != null) educationDays++;
        if (course.getDay5Date() != null) educationDays++;

        long totalMinutes = minutesPerDay * educationDays;
        return Math.round(totalMinutes * 0.8);
    }

    /**
     * 하루 교육 가능 시간(분) = (교육종료시간 - 교육시작시간) - 휴게시간(분).
     * 휴게시간이 없으면 0으로 취급해 기존과 동일하게 동작한다.
     */
    private long minutesPerDay(CourseEntity course) {
        long rawMinutes = Duration.between(
                        course.getEducationStartTime(),
                        course.getEducationEndTime())
                .toMinutes();

        if (course.getBreakMinutes() == null) {
            return rawMinutes;
        }
        return Math.max(rawMinutes - course.getBreakMinutes(), 0);
    }

    private CompletionRiskStatus calculateRiskStatus(
            long attendedMinutes,
            long requiredMinutes,
            CourseEntity course,
            List<AttendanceEntity> participantAttendances) {

        if (requiredMinutes <= 0) {
            return CompletionRiskStatus.UNKNOWN;
        }

        if (attendedMinutes >= requiredMinutes) {
            return CompletionRiskStatus.PASS;
        }

        // 이미 출결 기록이 존재하는 dayNo는 "확정된 날"로 보고 remainingDays에서 제외
        Set<Integer> recordedDays = participantAttendances.stream()
                .map(AttendanceEntity::getDayNo)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        long remainingDays = 0;

        if (course.getDay1Date() != null && !recordedDays.contains(1) && course.getDay1Date().isAfter(today)) remainingDays++;
        if (course.getDay2Date() != null && !recordedDays.contains(2) && course.getDay2Date().isAfter(today)) remainingDays++;
        if (course.getDay3Date() != null && !recordedDays.contains(3) && course.getDay3Date().isAfter(today)) remainingDays++;
        if (course.getDay4Date() != null && !recordedDays.contains(4) && course.getDay4Date().isAfter(today)) remainingDays++;
        if (course.getDay5Date() != null && !recordedDays.contains(5) && course.getDay5Date().isAfter(today)) remainingDays++;

        if (course.getEducationStartTime() == null || course.getEducationEndTime() == null) {
            return CompletionRiskStatus.FAIL;
        }

        long minutesPerDay = minutesPerDay(course);
        long maxPossibleMinutes = attendedMinutes + remainingDays * minutesPerDay;

        if (maxPossibleMinutes < requiredMinutes) {
            return CompletionRiskStatus.FAIL;
        }

        long shortage = requiredMinutes - attendedMinutes;
        if (shortage <= minutesPerDay) return CompletionRiskStatus.DANGER;
        if (shortage <= minutesPerDay * 2) return CompletionRiskStatus.WARNING;

        return CompletionRiskStatus.SAFE;
    }
}