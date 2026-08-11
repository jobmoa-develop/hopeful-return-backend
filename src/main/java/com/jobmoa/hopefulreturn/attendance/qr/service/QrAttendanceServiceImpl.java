package com.jobmoa.hopefulreturn.attendance.qr.service;

import com.jobmoa.hopefulreturn.attendance.entity.AttendanceEntity;
import com.jobmoa.hopefulreturn.attendance.entity.AttendanceStatus;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrHistoryResponse;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrLandingResponse;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrLeaveItem;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrLeaveRequest;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrLeaveReturnRequest;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrStatusResponse;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrVerifyRequest;
import com.jobmoa.hopefulreturn.attendance.repository.AttendanceRepository;
import com.jobmoa.hopefulreturn.attendanceleave.entity.AttendanceLeaveEntity;
import com.jobmoa.hopefulreturn.attendanceleave.repository.AttendanceLeaveRepository;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * QR 공개 입·퇴실 상태 머신 구현.
 *
 * <p>본인확인은 성명 + 전화번호 뒤 4자리(하이픈 제거)를 DB 에서 매칭하며, 해당 회차 등록자 중
 * 정확히 1명일 때만 통과시킨다. 불일치/동명이인 충돌 시 어느 필드가 틀렸는지 노출하지 않는
 * 일반화된 오류({@link ErrorCode#QR_VERIFICATION_FAILED})를 던진다.
 *
 * <p>오늘 일차(dayNo)는 {@code course.day1Date..day5Date} 와 오늘 날짜를 비교해 정한다.
 * 입실은 교육 시작 + {@link #LATE_GRACE_MINUTES}분 이내면 정상 출석(ATTEND), 초과면 지각(LATE)
 * 이며, 지각인 경우 [교육시작 ~ 실제 입실시각] 구간을 외출(attendance_leave)로 자동 기록한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class QrAttendanceServiceImpl implements QrAttendanceService {

    /** 입실 지각 유예(분). 교육 시작 + 이 시간 이내 입실은 정상 출석으로 인정한다. */
    private static final int LATE_GRACE_MINUTES = 10;

    /** 지각 시 자동 생성되는 외출 사유 문구. */
    private static final String AUTO_LATE_LEAVE_REASON = "지각(자동)";

    private final CourseRepository courseRepository;
    private final CourseParticipantRepository courseParticipantRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceLeaveRepository attendanceLeaveRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public QrLandingResponse getLanding(Long courseId) {
        CourseEntity course = getCourse(courseId);
        Integer dayNo = resolveDayNoOrNull(course, LocalDate.now(clock));
        String regionName = course.getRegion() == null ? null : course.getRegion().getName();
        return new QrLandingResponse(
                course.getCourseId(),
                course.getCourseName(),
                regionName,
                course.getCourseNumber(),
                course.getLocalCourseNumber(),
                dayNo,
                course.getEducationStartTime(),
                course.getEducationEndTime());
    }

    @Override
    @Transactional(readOnly = true)
    public QrStatusResponse verify(Long courseId, QrVerifyRequest request) {
        CourseEntity course = getCourse(courseId);
        int dayNo = resolveDayNo(course);
        CourseParticipantEntity cp = resolveCourseParticipant(courseId, request.name(), request.phoneLast4());
        return buildStatus(cp, course, dayNo);
    }

    @Override
    public QrStatusResponse checkIn(Long courseId, QrVerifyRequest request) {
        CourseEntity course = getCourse(courseId);
        int dayNo = resolveDayNo(course);
        CourseParticipantEntity cp = resolveCourseParticipant(courseId, request.name(), request.phoneLast4());

        LocalTime start = requireStartTime(course);
        AttendanceEntity attendance = findAttendance(cp.getCourseParticipantId(), dayNo);
        if (attendance != null && attendance.getCheckInTime() != null) {
            throw new BusinessException(ErrorCode.QR_ALREADY_CHECKED_IN);
        }

        LocalTime now = LocalTime.now(clock);
        boolean late = now.isAfter(start.plusMinutes(LATE_GRACE_MINUTES));
        AttendanceStatus status = late ? AttendanceStatus.LATE : AttendanceStatus.ATTEND;

        if (attendance == null) {
            attendance = AttendanceEntity.builder()
                    .courseParticipantId(cp.getCourseParticipantId())
                    .dayNo(dayNo)
                    .checkInTime(now)
                    .status(status)
                    .createdAt(LocalDateTime.now(clock))
                    .build();
        } else {
            attendance.setCheckInTime(now);
            attendance.setStatus(status);
        }
        attendance = attendanceRepository.save(attendance);

        // 지각이면 [교육시작 ~ 실제 입실시각] 을 외출로 자동 기록한다.
        if (late) {
            attendanceLeaveRepository.save(AttendanceLeaveEntity.builder()
                    .attendanceId(attendance.getAttendanceId())
                    .leaveTime(start)
                    .returnTime(now)
                    .reason(AUTO_LATE_LEAVE_REASON)
                    .createdAt(LocalDateTime.now(clock))
                    .build());
        }

        return buildStatus(cp, course, dayNo);
    }

    @Override
    public QrStatusResponse leave(Long courseId, QrLeaveRequest request) {
        CourseEntity course = getCourse(courseId);
        int dayNo = resolveDayNo(course);
        CourseParticipantEntity cp = resolveCourseParticipant(courseId, request.name(), request.phoneLast4());

        AttendanceEntity attendance = requireActiveAttendance(cp.getCourseParticipantId(), dayNo);
        attendanceLeaveRepository.save(AttendanceLeaveEntity.builder()
                .attendanceId(attendance.getAttendanceId())
                .leaveTime(request.leaveTime())
                .createdAt(LocalDateTime.now(clock))
                .build());

        return buildStatus(cp, course, dayNo);
    }

    @Override
    public QrStatusResponse leaveReturn(Long courseId, QrLeaveReturnRequest request) {
        CourseEntity course = getCourse(courseId);
        int dayNo = resolveDayNo(course);
        CourseParticipantEntity cp = resolveCourseParticipant(courseId, request.name(), request.phoneLast4());

        AttendanceEntity attendance = requireActiveAttendance(cp.getCourseParticipantId(), dayNo);
        List<AttendanceLeaveEntity> leaves = attendanceLeaveRepository.findByAttendanceId(attendance.getAttendanceId());

        AttendanceLeaveEntity target = resolveReturnTarget(leaves, request.attendanceLeaveId());
        target.setReturnTime(request.returnTime());
        attendanceLeaveRepository.save(target);

        return buildStatus(cp, course, dayNo);
    }

    @Override
    public QrStatusResponse checkOut(Long courseId, QrVerifyRequest request) {
        CourseEntity course = getCourse(courseId);
        int dayNo = resolveDayNo(course);
        CourseParticipantEntity cp = resolveCourseParticipant(courseId, request.name(), request.phoneLast4());

        AttendanceEntity attendance = requireActiveAttendance(cp.getCourseParticipantId(), dayNo);
        LocalTime end = requireEndTime(course);
        LocalTime now = LocalTime.now(clock);
        if (now.isBefore(end)) {
            throw new BusinessException(ErrorCode.QR_CHECKOUT_BEFORE_END);
        }
        attendance.setCheckOutTime(now);
        attendanceRepository.save(attendance);

        return buildStatus(cp, course, dayNo);
    }

    @Override
    @Transactional(readOnly = true)
    public QrHistoryResponse history(Long courseId, QrVerifyRequest request) {
        CourseEntity course = getCourse(courseId);
        CourseParticipantEntity cp = resolveCourseParticipant(courseId, request.name(), request.phoneLast4());

        List<AttendanceEntity> attendances =
                attendanceRepository.findByCourseParticipantId(cp.getCourseParticipantId());
        Map<Long, List<AttendanceLeaveEntity>> leavesByAttendanceId = attendances.isEmpty()
                ? Map.of()
                : attendanceLeaveRepository.findByAttendanceIdIn(
                        attendances.stream().map(AttendanceEntity::getAttendanceId).toList())
                .stream()
                .collect(Collectors.groupingBy(AttendanceLeaveEntity::getAttendanceId));

        List<QrHistoryResponse.DayItem> days = attendances.stream()
                .sorted(Comparator.comparing(AttendanceEntity::getDayNo,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(a -> new QrHistoryResponse.DayItem(
                        a.getDayNo(),
                        dayNoToDate(course, a.getDayNo()),
                        a.getCheckInTime(),
                        a.getCheckOutTime(),
                        a.getStatus() == null ? null : a.getStatus().name(),
                        toLeaveItems(leavesByAttendanceId.getOrDefault(a.getAttendanceId(), List.of()))))
                .toList();

        return new QrHistoryResponse(participantName(cp), days);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────

    private CourseEntity getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
    }

    /**
     * 성명 + 전화번호 뒤 4자리로 해당 회차 등록자 중 정확히 1명을 찾는다.
     * 0명(불일치) 또는 2명 이상(동명이인 + 동일 뒷자리)이면 일반화된 본인확인 오류를 던진다.
     */
    private CourseParticipantEntity resolveCourseParticipant(Long courseId, String name, String phoneLast4) {
        String trimmedName = name == null ? null : name.trim();
        List<CourseParticipantEntity> matched =
                courseParticipantRepository.findForQrVerify(courseId, trimmedName, phoneLast4);
        if (matched.size() != 1) {
            throw new BusinessException(ErrorCode.QR_VERIFICATION_FAILED);
        }
        return matched.get(0);
    }

    /** 오늘 날짜가 회차의 어느 일차인지 판정한다. 교육일이 아니면 예외. */
    private int resolveDayNo(CourseEntity course) {
        Integer dayNo = resolveDayNoOrNull(course, LocalDate.now(clock));
        if (dayNo == null) {
            throw new BusinessException(ErrorCode.QR_NOT_CLASS_DAY);
        }
        return dayNo;
    }

    /** 주어진 날짜가 회차의 어느 일차인지 반환한다(없으면 null). */
    private Integer resolveDayNoOrNull(CourseEntity course, LocalDate date) {
        LocalDate[] dayDates = {
                course.getDay1Date(), course.getDay2Date(), course.getDay3Date(),
                course.getDay4Date(), course.getDay5Date()
        };
        for (int i = 0; i < dayDates.length; i++) {
            if (date.equals(dayDates[i])) {
                return i + 1;
            }
        }
        return null;
    }

    private LocalDate dayNoToDate(CourseEntity course, Integer dayNo) {
        if (dayNo == null) {
            return null;
        }
        return switch (dayNo) {
            case 1 -> course.getDay1Date();
            case 2 -> course.getDay2Date();
            case 3 -> course.getDay3Date();
            case 4 -> course.getDay4Date();
            case 5 -> course.getDay5Date();
            default -> null;
        };
    }

    private LocalTime requireStartTime(CourseEntity course) {
        LocalTime start = course.getEducationStartTime();
        if (start == null) {
            throw new BusinessException(ErrorCode.COURSE_EDUCATION_START_TIME_NOT_SET);
        }
        return start;
    }

    private LocalTime requireEndTime(CourseEntity course) {
        LocalTime end = course.getEducationEndTime();
        if (end == null) {
            throw new BusinessException(ErrorCode.COURSE_EDUCATION_END_TIME_NOT_SET);
        }
        return end;
    }

    /** (courseParticipantId, dayNo) 출석 1건을 반환한다(없으면 null). */
    private AttendanceEntity findAttendance(Long courseParticipantId, int dayNo) {
        return attendanceRepository.findByCourseParticipantIdAndDayNo(courseParticipantId, dayNo)
                .stream().findFirst().orElse(null);
    }

    private AttendanceEntity requireCheckedIn(Long courseParticipantId, int dayNo) {
        AttendanceEntity attendance = findAttendance(courseParticipantId, dayNo);
        if (attendance == null || attendance.getCheckInTime() == null) {
            throw new BusinessException(ErrorCode.QR_NOT_CHECKED_IN);
        }
        return attendance;
    }

    /** 입실 완료 + 아직 퇴실 전인 출석을 요구한다. 조퇴·외출·퇴실은 퇴실 이후 불가. */
    private AttendanceEntity requireActiveAttendance(Long courseParticipantId, int dayNo) {
        AttendanceEntity attendance = requireCheckedIn(courseParticipantId, dayNo);
        if (attendance.getCheckOutTime() != null) {
            throw new BusinessException(ErrorCode.QR_ALREADY_CHECKED_OUT);
        }
        return attendance;
    }

    /** 복귀 대상 조퇴 건을 고른다. id 지정 시 소속 검증, 미지정 시 복귀 미기록 최신 건. */
    private AttendanceLeaveEntity resolveReturnTarget(List<AttendanceLeaveEntity> leaves, Long attendanceLeaveId) {
        if (attendanceLeaveId != null) {
            return leaves.stream()
                    .filter(l -> attendanceLeaveId.equals(l.getAttendanceLeaveId()))
                    .filter(l -> l.getReturnTime() == null)  // 이미 복귀한 건은 덮어쓰지 않음
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.ATTENDANCE_LEAVE_NOT_FOUND));
        }
        return leaves.stream()
                .filter(l -> l.getReturnTime() == null)
                .max(Comparator.comparing(AttendanceLeaveEntity::getAttendanceLeaveId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTENDANCE_LEAVE_NOT_FOUND));
    }

    private QrStatusResponse buildStatus(CourseParticipantEntity cp, CourseEntity course, int dayNo) {
        AttendanceEntity attendance = findAttendance(cp.getCourseParticipantId(), dayNo);
        List<QrLeaveItem> leaves = attendance == null
                ? List.of()
                : toLeaveItems(attendanceLeaveRepository.findByAttendanceId(attendance.getAttendanceId()));

        boolean checkedIn = attendance != null && attendance.getCheckInTime() != null;
        boolean checkedOut = attendance != null && attendance.getCheckOutTime() != null;
        LocalTime end = course.getEducationEndTime();
        boolean afterEnd = end != null && !LocalTime.now(clock).isBefore(end);

        boolean canCheckIn = !checkedIn;
        boolean canLeave = checkedIn && !checkedOut;
        boolean canCheckOut = checkedIn && !checkedOut && afterEnd;

        return new QrStatusResponse(
                participantName(cp),
                dayNo,
                attendance == null ? null : attendance.getCheckInTime(),
                attendance == null ? null : attendance.getCheckOutTime(),
                attendance == null || attendance.getStatus() == null ? null : attendance.getStatus().name(),
                leaves,
                canCheckIn,
                canLeave,
                canCheckOut);
    }

    private List<QrLeaveItem> toLeaveItems(List<AttendanceLeaveEntity> leaves) {
        return leaves.stream()
                .sorted(Comparator.comparing(AttendanceLeaveEntity::getAttendanceLeaveId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(l -> new QrLeaveItem(
                        l.getAttendanceLeaveId(),
                        l.getLeaveTime(),
                        l.getReturnTime(),
                        l.getReason()))
                .toList();
    }

    private String participantName(CourseParticipantEntity cp) {
        ParticipantEntity participant = cp.getParticipant();
        return participant == null ? null : participant.getName();
    }
}
