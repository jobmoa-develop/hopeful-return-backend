package com.jobmoa.hopefulreturn.course.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.model.dto.CourseDetailResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseStaffListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseStaffSmsHistoryResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CreateCourseRequest;
import com.jobmoa.hopefulreturn.course.model.dto.CreateCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.DeleteCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.NotifyCourseChangeRequest;
import com.jobmoa.hopefulreturn.course.model.dto.NotifyCourseChangeResponse;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseRequest;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseStatusRequest;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseStatusResponse;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.course.scope.CourseScope;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.StaffRole;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.CourseStaffSmsEntity;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffNotifyType;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffSmsSendStatus;
import com.jobmoa.hopefulreturn.coursestaffsms.repository.CourseStaffSmsRepository;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import com.jobmoa.hopefulreturn.sms.SmsSendCommand;
import com.jobmoa.hopefulreturn.sms.SmsSendResult;
import com.jobmoa.hopefulreturn.sms.SmsService;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import jakarta.persistence.criteria.Predicate;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime; // LocalTime import 추가
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseServiceImpl implements CourseService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final DateTimeFormatter NOTIFICATION_DATE_FORMAT = DateTimeFormatter.ofPattern("M/d");

    // 담당자 안내 문자(상태변경·일정변경) 발송 형식 판별용 — 참여자용(ParticipantSmsServiceImpl)과 동일 기준(EUC-KR 90바이트).
    private static final Charset EUC_KR = Charset.forName("EUC-KR");
    private static final int SMS_MAX_BYTES = 90;

    private final CourseRepository courseRepository;
    private final RegionRepository regionRepository;
    private final CourseParticipantRepository courseParticipantRepository;
    private final CourseStaffRepository courseStaffRepository;
    private final UsersRepository usersRepository;
    private final SmsService smsService;
    private final CourseStaffSmsRepository courseStaffSmsRepository;

    @Override
    public CreateCourseResponse create(CreateCourseRequest request, Long createdBy) {
        regionRepository.findById(request.regionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        CourseEntity course = CourseEntity.builder()
                .regionId(request.regionId())
                .courseNumber(request.courseNumber())
                .localCourseNumber(request.localCourseNumber())
                .courseName(request.courseName())
                .recruitStart(request.recruitStart())
                .recruitEnd(request.recruitEnd())
                .day1Date(request.day1Date())
                .day2Date(request.day2Date())
                .day3Date(request.day3Date())
                .day4Date(request.day4Date())
                .day5Date(request.day5Date())
                .educationStartTime(request.educationStartTime())
                .educationEndTime(request.educationEndTime())
                .breakMinutes(request.breakMinutes())
                .capacity(request.capacity())
                .minimumCapacity(request.minimumCapacity())
                .location(request.location())
                .status(CourseStatus.PLANNED)
                .planSubmitDate(request.planSubmitDate())
                .createdBy(createdBy)
                .createdAt(now)
                .updatedAt(now)
                .build();

        CourseEntity savedCourse = courseRepository.save(course);
        return new CreateCourseResponse(
                savedCourse.getCourseId(),
                savedCourse.getLocalCourseNumber(),
                savedCourse.getStatus().name());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseListResponse findAll(
            Long regionId,
            Long parentRegionId,
            String status,
            String keyword,
            CourseScope scope,
            String sortBy,
            String sortOrder,
            Integer page,
            Integer size) {
        Pageable pageable = PageRequest.of(
                sanitizePage(page),
                sanitizeSize(size),
                resolveCourseSort(sortBy, sortOrder));
        List<Long> regionIds = resolveRegionIds(regionId, parentRegionId);
        Page<CourseEntity> courses = courseRepository.findAll(
                buildSpecification(regionIds, parseStatus(status), normalize(keyword), scope),
                pageable);
        // 회차별 참여자 수는 group by 배치 1쿼리로 조회(강좌마다 findByCourseId().size() 호출 N+1 제거).
        List<Long> courseIds = courses.getContent().stream()
                .map(CourseEntity::getCourseId)
                .toList();
        Map<Long, Integer> participantCounts = participantCountsByCourseId(courseIds);
        List<CourseListResponse.Item> content = courses.getContent().stream()
                .map(course -> toListItem(
                        course,
                        participantCounts.getOrDefault(course.getCourseId(), 0)))
                .toList();

        return new CourseListResponse(
                content,
                courses.getNumber(),
                courses.getSize(),
                courses.getTotalElements(),
                courses.getTotalPages());
    }

    // 강좌 목록 정렬 화이트리스트 — 키(FE 전달값) → JPA 엔티티 속성 경로. 원문 sortBy 는 정렬에 쓰지 않는다.
    // currentParticipants(배치 group-by 계산)는 엔티티 속성이 아니라 제외한다.
    private static final Map<String, String> COURSE_SORT_WHITELIST = Map.of(
            "courseName", "courseName",
            "regionName", "region.name",
            "courseNumber", "courseNumber",
            "localCourseNumber", "localCourseNumber",
            "capacity", "capacity",
            "status", "status",
            "day1Date", "day1Date",
            "planSubmitDate", "planSubmitDate");

    // sortBy/sortOrder 를 화이트리스트로 검증해 Sort 를 구성한다. 미허용/미지정이면 기존 기본 정렬(courseId ASC)로
    // 폴백하며, 정렬 지정 시엔 courseId 를 tiebreaker 로 붙여 결정적 순서를 보장한다.
    private Sort resolveCourseSort(String sortBy, String sortOrder) {
        String property = sortBy == null ? null : COURSE_SORT_WHITELIST.get(sortBy);
        if (property == null) {
            return Sort.by(Sort.Direction.ASC, "courseId");
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder == null ? null : sortOrder.trim())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(new Sort.Order(direction, property), Sort.Order.asc("courseId"));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDetailResponse findById(Long courseId) {
        CourseEntity course = findCourse(courseId);
        return toDetailResponse(course);
    }

    @Override
    public UpdateCourseResponse update(Long courseId, UpdateCourseRequest request) {
        CourseEntity course = findCourse(courseId);

        if (request.regionId() != null) {
            regionRepository.findById(request.regionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));
            course.setRegionId(request.regionId());
        }
        if (request.courseNumber() != null) {
            course.setCourseNumber(request.courseNumber());
        }
        if (request.localCourseNumber() != null) {
            course.setLocalCourseNumber(request.localCourseNumber());
        }
        if (request.courseName() != null) {
            course.setCourseName(request.courseName());
        }
        if (request.recruitStart() != null) {
            course.setRecruitStart(request.recruitStart());
        }
        if (request.recruitEnd() != null) {
            course.setRecruitEnd(request.recruitEnd());
        }
        if (request.day1Date() != null) {
            course.setDay1Date(request.day1Date());
        }
        if (request.day2Date() != null) {
            course.setDay2Date(request.day2Date());
        }
        if (request.day3Date() != null) {
            course.setDay3Date(request.day3Date());
        }
        if (request.day4Date() != null) {
            course.setDay4Date(request.day4Date());
        }
        if (request.day5Date() != null) {
            course.setDay5Date(request.day5Date());
        }
        if (request.educationStartTime() != null) {
            course.setEducationStartTime(request.educationStartTime());
        }
        if (request.educationEndTime() != null) {
            course.setEducationEndTime(request.educationEndTime());
        }
        if (request.breakMinutes() != null) {
            course.setBreakMinutes(request.breakMinutes());
        }
        if (request.capacity() != null) {
            course.setCapacity(request.capacity());
        }
        if (request.minimumCapacity() != null) {
            course.setMinimumCapacity(request.minimumCapacity());
        }
        if (request.location() != null) {
            course.setLocation(request.location());
        }
        if (request.planSubmitDate() != null) {
            course.setPlanSubmitDate(request.planSubmitDate());
        }
        course.setUpdatedAt(LocalDateTime.now());

        courseRepository.save(course);
        return new UpdateCourseResponse(course.getCourseId(), course.getLocalCourseNumber(), true);
    }

    @Override
    public UpdateCourseStatusResponse updateStatus(Long courseId, UpdateCourseStatusRequest request, Long changedBy) {
        CourseEntity course = findCourse(courseId);
        CourseStatus oldStatus = course.getStatus();
        CourseStatus newStatus = parseStatus(request.status());
        validateEducationTimesForActiveStatus(course, newStatus);

        course.setStatus(newStatus);
        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);

        // 모집마감(CLOSED)·취소(CANCELED)로 "새로 전환"될 때만 담당자에게 문자 안내를 보낸다.
        // 발송 자체가 실패해도 강좌 상태 변경(핵심 처리)은 되돌아가지 않도록 예외를 여기서 흡수한다.
        if (oldStatus != newStatus && requiresStaffNotification(newStatus)) {
            notifyStaffsOfStatusChange(course, newStatus, changedBy);
        }

        return new UpdateCourseStatusResponse(course.getCourseId(), course.getStatus().name());
    }

    /**
     * 회차 상태가 모집마감(CLOSED)·취소(CANCELED)로 바뀌면, 해당 회차 담당자(PM 제외)에게
     * 개강/폐강 확정 안내 SMS를 발송한다. 발송 실패는 로그만 남기고 상태 변경 트랜잭션에는 영향을 주지 않는다.
     * 발송 시도(성공/실패 모두) 결과는 course_staff_sms 에 담당자별로 한 행씩 기록하며,
     * sentBy 에는 이 상태변경을 트리거한 계정(changedBy)을 기록해 "누가 이 문자를 유발했는지"를 남긴다.
     */
    private void notifyStaffsOfStatusChange(CourseEntity course, CourseStatus newStatus, Long changedBy) {
        try {
            List<Long> targetUserIds = courseStaffRepository.findByCourseId(course.getCourseId()).stream()
                    .filter(staff -> staff.getStaffRole() != StaffRole.PROJECT_MANAGER)
                    .map(CourseStaffEntity::getUserId)
                    .distinct()
                    .toList();
            if (targetUserIds.isEmpty()) {
                return;
            }

            List<UsersEntity> targetUsers = usersRepository.findAllById(targetUserIds).stream()
                    .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                    .filter(user -> StringUtils.hasText(user.getPhone()))
                    .toList();
            if (targetUsers.isEmpty()) {
                log.warn("[CourseStatus] 알림 대상 담당자의 전화번호가 없습니다. courseId={}", course.getCourseId());
                return;
            }

            String content = buildStatusChangeMessage(course, newStatus);
            List<SmsSendCommand.Recipient> recipients = targetUsers.stream()
                    .map(user -> new SmsSendCommand.Recipient(user.getPhone(), content))
                    .toList();

            // 90바이트(EUC-KR) 이하면 SMS, 초과하면 LMS로 자동 판별해 불필요한 LMS 과금을 피한다.
            SmsSendResult result = smsService.send(new SmsSendCommand(
                    resolveMessageFormat(content), null, content, recipients, null, null, null));

            saveStaffSmsLog(course, targetUsers, StaffNotifyType.STATUS_CHANGE, content, result, changedBy);
        } catch (RuntimeException e) {
            log.warn("[CourseStatus] 담당자 상태변경 알림 발송에 실패했습니다. courseId={}", course.getCourseId(), e);
        }
    }

    private boolean requiresStaffNotification(CourseStatus status) {
        return status == CourseStatus.CLOSED || status == CourseStatus.CANCELED;
    }

    /**
     * "[잡모아]\n양천 15회차(8/10 ~ 8/15) "폐강확정" 되었습니다." 형식.
     * - 지역명: RegionEntity.name(하위 지역명만, 예: "양천")
     * - 확정 문구: CANCELED → "폐강확정", CLOSED(모집마감) → "개강확정"
     * - 기간: day1Date ~ (day2~day5 중 채워진 마지막 날짜). 비어있으면 괄호를 생략한다.
     */
    private String buildStatusChangeMessage(CourseEntity course, CourseStatus status) {
        String regionName = extractRegionName(course);
        String regionText = StringUtils.hasText(regionName) ? regionName + " " : "";
        String confirmText = status == CourseStatus.CANCELED ? "폐강확정" : "개강확정";
        String periodText = buildEducationPeriodText(course);

        return "[잡모아]\n"
                + regionText + course.getLocalCourseNumber() + "회차" + periodText
                + " \"" + confirmText + "\" 되었습니다.";
    }

    @Override
    public NotifyCourseChangeResponse notifyScheduleChange(
            Long courseId, NotifyCourseChangeRequest request, Long sentBy) {
        CourseEntity course = findCourse(courseId);

        // 요청받은 userId 중, 이 회차에 실제 배치된 담당자(PM 제외)만 대상으로 한다(임의 대상 발송 방지).
        Set<Long> assignedNonPmUserIds = courseStaffRepository.findByCourseId(courseId).stream()
                .filter(staff -> staff.getStaffRole() != StaffRole.PROJECT_MANAGER)
                .map(CourseStaffEntity::getUserId)
                .collect(Collectors.toSet());

        List<Long> targetUserIds = request.userIds().stream()
                .distinct()
                .filter(assignedNonPmUserIds::contains)
                .toList();
        if (targetUserIds.isEmpty()) {
            return new NotifyCourseChangeResponse(0);
        }

        List<UsersEntity> targetUsers = usersRepository.findAllById(targetUserIds).stream()
                .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                .filter(user -> StringUtils.hasText(user.getPhone()))
                .toList();
        if (targetUsers.isEmpty()) {
            return new NotifyCourseChangeResponse(0);
        }

        String content = buildScheduleChangeMessage(course);
        List<SmsSendCommand.Recipient> recipients = targetUsers.stream()
                .map(user -> new SmsSendCommand.Recipient(user.getPhone(), content))
                .toList();

        // 90바이트(EUC-KR) 이하면 SMS, 초과하면 LMS로 자동 판별해 불필요한 LMS 과금을 피한다.
        SmsSendResult result = smsService.send(new SmsSendCommand(
                resolveMessageFormat(content), null, content, recipients, null, null, null));

        saveStaffSmsLog(course, targetUsers, StaffNotifyType.SCHEDULE_CHANGE, content, result, sentBy);

        return new NotifyCourseChangeResponse(targetUsers.size());
    }

    /**
     * 담당자 한 명당 한 행씩 발송 시도 결과(성공/실패)를 course_staff_sms 에 기록한다.
     * DB 저장 자체가 실패해도 문자 발송(핵심 처리)에는 영향을 주지 않도록 예외를 흡수한다.
     */
    private void saveStaffSmsLog(
            CourseEntity course,
            List<UsersEntity> targetUsers,
            StaffNotifyType notifyType,
            String content,
            SmsSendResult result,
            Long sentBy) {
        try {
            LocalDateTime now = LocalDateTime.now();
            StaffSmsSendStatus status = (result != null && result.success())
                    ? StaffSmsSendStatus.SUCCESS
                    : StaffSmsSendStatus.FAIL;

            for (UsersEntity user : targetUsers) {
                courseStaffSmsRepository.save(CourseStaffSmsEntity.builder()
                        .courseId(course.getCourseId())
                        .userId(user.getUserId())
                        .sentBy(sentBy)
                        .notifyType(notifyType)
                        .content(content)
                        .sendStatus(status)
                        .sentAt(now)
                        .createdAt(now)
                        .build());
            }
        } catch (RuntimeException e) {
            log.warn("[CourseStaffSms] 담당자 알림 발송 이력 저장에 실패했습니다. courseId={}, notifyType={}",
                    course.getCourseId(), notifyType, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CourseStaffSmsHistoryResponse findStaffSmsHistory(Long courseId) {
        findCourse(courseId);
        List<CourseStaffSmsEntity> rows = courseStaffSmsRepository.findByCourseIdWithUsers(courseId);
        List<CourseStaffSmsHistoryResponse.Item> content = rows.stream()
                .map(row -> new CourseStaffSmsHistoryResponse.Item(
                        row.getCourseStaffSmsId(),
                        row.getUserId(),
                        row.getRecipient() == null ? null : row.getRecipient().getName(),
                        row.getSentBy(),
                        row.getSender() == null ? null : row.getSender().getName(),
                        row.getNotifyType() == null ? null : row.getNotifyType().name(),
                        row.getContent(),
                        row.getSendStatus() == null ? null : row.getSendStatus().name(),
                        row.getSentAt()))
                .toList();
        return new CourseStaffSmsHistoryResponse(content);
    }

    /**
     * "[잡모아]\n양천지역 15회차(8/10~)\n일정/장소 정보 변경\n시스템에서 확인 부탁드립니다" 형식.
     * 강좌 수정(교육일·교육시간·휴게시간·교육장 변경)에서 사용하는 안내 문구.
     */
    private String buildScheduleChangeMessage(CourseEntity course) {
        String regionName = extractRegionName(course);
        String regionText = StringUtils.hasText(regionName) ? regionName + "지역 " : "";
        String startDateText = buildScheduleStartDateText(course);

        return "[잡모아]\n"
                + regionText + course.getLocalCourseNumber() + "회차" + startDateText + "\n"
                + "일정/장소 정보 변경\n"
                + "시스템에서 확인 부탁드립니다";
    }

    /**
     * "(8/10~)" 형태 — 시작일(day1Date)만 표시하고 끝에 물결표를 붙인다. day1Date가 없으면 빈 문자열.
     */
    private String buildScheduleStartDateText(CourseEntity course) {
        LocalDate start = course.getDay1Date();
        if (start == null) {
            return "";
        }
        return "(" + start.format(NOTIFICATION_DATE_FORMAT) + "~)";
    }

    /**
     * "(8/10 ~ 8/15)" 형태의 교육 기간 표기를 만든다. day1Date가 없으면 빈 문자열,
     * 종료일이 없으면 시작일만 표기한다(예: "(8/10)").
     */
    private String buildEducationPeriodText(CourseEntity course) {
        LocalDate start = course.getDay1Date();
        if (start == null) {
            return "";
        }
        LocalDate end = lastNonNull(course.getDay5Date(), course.getDay4Date(),
                course.getDay3Date(), course.getDay2Date());
        if (end == null || end.equals(start)) {
            return "(" + start.format(NOTIFICATION_DATE_FORMAT) + ")";
        }
        return "(" + start.format(NOTIFICATION_DATE_FORMAT) + " ~ " + end.format(NOTIFICATION_DATE_FORMAT) + ")";
    }

    private LocalDate lastNonNull(LocalDate... dates) {
        for (LocalDate date : dates) {
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    // 담당자 안내 문자 발송 형식 판별: EUC-KR 기준 90바이트 이하면 SMS, 초과하면 LMS.
    private String resolveMessageFormat(String content) {
        return byteLength(content) <= SMS_MAX_BYTES ? "SMS" : "LMS";
    }

    private int byteLength(String value) {
        return value == null ? 0 : value.getBytes(EUC_KR).length;
    }

    @Override
    public DeleteCourseResponse delete(Long courseId) {
        CourseEntity course = findCourse(courseId);
        courseRepository.delete(course);
        return new DeleteCourseResponse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseParticipantListResponse findParticipants(
            Long courseId,
            String status,
            String keyword,
            CourseScope scope,
            Integer page,
            Integer size) {
        findCourse(courseId);
        if (scope != null && !scope.allowsCourse(courseId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Pageable pageable = PageRequest.of(
                sanitizePage(page),
                sanitizeSize(size),
                Sort.by(Sort.Direction.ASC, "courseParticipantId"));
        CourseParticipantStatus parsedStatus = parseParticipantStatus(status);
        String normalizedKeyword = normalize(keyword);
        Page<CourseParticipantEntity> participants = courseParticipantRepository.findPageByCourseIdAndFilters(
                courseId, parsedStatus, normalizedKeyword, pageable);
        List<CourseParticipantListResponse.Item> content = participants.getContent().stream()
                .map(this::toParticipantListItem)
                .toList();

        return new CourseParticipantListResponse(
                content,
                participants.getNumber(),
                participants.getSize(),
                participants.getTotalElements(),
                participants.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseStaffListResponse findStaffs(Long courseId) {
        findCourse(courseId);

        List<CourseStaffListResponse.Item> staffs = courseStaffRepository.findByCourseId(courseId).stream()
                .map(this::toStaffListItem)
                .toList();
        return new CourseStaffListResponse(staffs);
    }

    private List<Long> resolveRegionIds(Long regionId, Long parentRegionId) {
        if (regionId != null) {
            return List.of(regionId);
        }
        if (parentRegionId != null) {
            return regionRepository.findByParentRegionId(parentRegionId).stream()
                    .map(RegionEntity::getRegionId)
                    .toList();
        }
        return null;
    }

    private Specification<CourseEntity> buildSpecification(
            List<Long> regionIds, CourseStatus status, String keyword, CourseScope scope) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (scope != null && !scope.unrestricted()) {
                if (scope.courseIds().isEmpty()) {
                    predicates.add(criteriaBuilder.disjunction());
                } else {
                    predicates.add(root.get("courseId").in(scope.courseIds()));
                }
            }
            if (regionIds != null) {
                if (regionIds.isEmpty()) {
                    predicates.add(criteriaBuilder.disjunction());
                } else {
                    predicates.add(root.get("regionId").in(regionIds));
                }
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(keyword)) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("courseName")),
                        "%" + keyword.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // 회차별 참여자 수를 group by 로 한 번에 조회해 Map<courseId, count> 로 변환한다.
    private Map<Long, Integer> participantCountsByCourseId(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : courseParticipantRepository.countByCourseIdIn(courseIds)) {
            counts.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return counts;
    }

    private CourseListResponse.Item toListItem(CourseEntity course, int participantCount) {
        return new CourseListResponse.Item(
                course.getCourseId(),
                course.getCourseName(),
                course.getCourseNumber(),
                course.getLocalCourseNumber(),
                extractRegionName(course),
                course.getStatus() == null ? null : course.getStatus().name(),
                course.getCapacity(),
                participantCount,
                deriveYear(course.getDay1Date()),
                course.getDay1Date(),
                course.getDay2Date(),
                course.getDay3Date(),
                course.getDay4Date(),
                course.getDay5Date(),
                course.getBreakMinutes(),
                course.getLocation(),
                course.getPlanSubmitDate());
    }

    private Integer deriveYear(LocalDate day1Date) {
        return day1Date == null ? null : day1Date.getYear();
    }

    private CourseDetailResponse toDetailResponse(CourseEntity course) {
        int currentParticipants =
                (int) courseParticipantRepository.countByCourseId(course.getCourseId());

        // 생성자 파라미터 세트에 맞춰 엔티티 내부 필드 값 추가 바인딩
        return new CourseDetailResponse(
                course.getCourseId(),
                course.getRegionId(),
                extractRegionName(course),
                course.getCourseNumber(),
                course.getLocalCourseNumber(),
                course.getCourseName(),
                course.getStatus() == null ? null : course.getStatus().name(),
                course.getCapacity(),
                course.getMinimumCapacity(),
                currentParticipants,
                course.getLocation(),
                course.getPlanSubmitDate(),

                deriveYear(course.getDay1Date()),


                // 추가된 날짜 및 시간 데이터 연동
                course.getRecruitStart(),
                course.getRecruitEnd(),

                course.getDay1Date(),
                course.getDay2Date(),
                course.getDay3Date(),
                course.getDay4Date(),
                course.getDay5Date(),
                course.getEducationStartTime(),
                course.getEducationEndTime(),
                course.getBreakMinutes()
        );

    }

    private CourseParticipantListResponse.Item toParticipantListItem(CourseParticipantEntity courseParticipant) {
        return new CourseParticipantListResponse.Item(
                courseParticipant.getCourseParticipantId(),
                courseParticipant.getParticipant() == null ? null : courseParticipant.getParticipant().getName(),
                courseParticipant.getParticipant() == null ? null : courseParticipant.getParticipant().getPhone(),
                courseParticipant.getStatus() == null ? null : courseParticipant.getStatus().name());
    }

    private CourseStaffListResponse.Item toStaffListItem(CourseStaffEntity courseStaff) {
        return new CourseStaffListResponse.Item(
                courseStaff.getCourseStaffId(),
                courseStaff.getUserId(),
                courseStaff.getUser() == null ? null : courseStaff.getUser().getName(),
                courseStaff.getStaffRole() == null ? null : courseStaff.getStaffRole().name(),
                courseStaff.getSessionType() == null ? null : courseStaff.getSessionType().name());
    }

    private CourseEntity findCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
    }

    private String extractRegionName(CourseEntity course) {
        return course.getRegion() == null ? null : course.getRegion().getName();
    }

    private CourseStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return CourseStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateEducationTimesForActiveStatus(CourseEntity course, CourseStatus status) {
        if (!requiresEducationTimes(status)) {
            return;
        }
        if (course.getEducationStartTime() == null) {
            throw new BusinessException(
                    ErrorCode.COURSE_EDUCATION_START_TIME_NOT_SET,
                    courseEducationStartTimeMissingMessage(course.getCourseId()));
        }
        if (course.getEducationEndTime() == null) {
            throw new BusinessException(
                    ErrorCode.COURSE_EDUCATION_END_TIME_NOT_SET,
                    courseEducationEndTimeMissingMessage(course.getCourseId()));
        }
    }

    private boolean requiresEducationTimes(CourseStatus status) {
        return status == CourseStatus.OPEN
                || status == CourseStatus.RECRUITING
                || status == CourseStatus.IN_PROGRESS;
    }

    private String courseEducationStartTimeMissingMessage(Long courseId) {
        return "해당 강좌(courseId=" + courseId + ")에 교육 시작 시간이 등록되어 있지 않아 강좌 상태를 변경할 수 없습니다. 강좌 정보를 먼저 등록해주세요.";
    }

    private String courseEducationEndTimeMissingMessage(Long courseId) {
        return "해당 강좌(courseId=" + courseId + ")에 교육 종료 시간이 등록되어 있지 않아 강좌 상태를 변경할 수 없습니다. 강좌 정보를 먼저 등록해주세요.";
    }

    private CourseParticipantStatus parseParticipantStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return CourseParticipantStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
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