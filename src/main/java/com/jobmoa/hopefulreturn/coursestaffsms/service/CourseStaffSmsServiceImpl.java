package com.jobmoa.hopefulreturn.coursestaffsms.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.StaffRole;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.CourseStaffSmsEntity;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffNotifyType;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffSmsSendStatus;
import com.jobmoa.hopefulreturn.coursestaffsms.model.dto.CourseStaffSmsPageResponse;
import com.jobmoa.hopefulreturn.coursestaffsms.model.dto.SendCourseStaffSmsRequest;
import com.jobmoa.hopefulreturn.coursestaffsms.model.dto.SendCourseStaffSmsResponse;
import com.jobmoa.hopefulreturn.coursestaffsms.repository.CourseStaffSmsRepository;
import com.jobmoa.hopefulreturn.region.support.RegionResolver;
import com.jobmoa.hopefulreturn.sms.SmsSendCommand;
import com.jobmoa.hopefulreturn.sms.SmsSendResult;
import com.jobmoa.hopefulreturn.sms.SmsService;
import com.jobmoa.hopefulreturn.sms.support.SmsByteCalculator;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseStaffSmsServiceImpl implements CourseStaffSmsService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final int BATCH_SIZE = 100;
    private static final String REGION_PLACEHOLDER = "{region}";
    private static final String ROUND_PLACEHOLDER = "{round}";
    private static final String ROLE_PLACEHOLDER = "{role}";
    private static final String START_DATE_PLACEHOLDER = "{startDate}";

    private final CourseStaffSmsRepository courseStaffSmsRepository;
    private final RegionResolver regionResolver;
    private final CourseRepository courseRepository;
    private final UsersRepository usersRepository;
    private final CourseStaffRepository courseStaffRepository;
    private final SmsService smsService;

    @Override
    @Transactional
    public SendCourseStaffSmsResponse send(Long sentBy, SendCourseStaffSmsRequest request) {
        if (request.groups() == null || request.groups().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        CourseEntity course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        String region = course.getRegion() == null ? null : course.getRegion().getName();
        // {round}=지역회차(localCourseNumber) 우선, 없으면 전체회차(courseNumber).
        Integer round = course.getLocalCourseNumber() != null
                ? course.getLocalCourseNumber()
                : course.getCourseNumber();
        // {startDate}=개강일(day1) "M/d" (예: 8/18). 없으면 빈 문자열.
        String startDate = course.getDay1Date() == null
                ? ""
                : course.getDay1Date().getMonthValue() + "/" + course.getDay1Date().getDayOfMonth();

        // 강좌 배정 역할 맵(userId → 역할 라벨). 동일 userId 다중 역할이면 첫 역할 유지.
        Map<Long, String> roleLabelByUser = new HashMap<>();
        for (CourseStaffEntity cs : courseStaffRepository.findByCourseId(request.courseId())) {
            roleLabelByUser.putIfAbsent(cs.getUserId(), roleLabel(cs.getStaffRole()));
        }

        // 수신자 users 로드(성명/전화)
        List<Long> allUserIds = request.groups().stream()
                .flatMap(g -> g.recipients().stream())
                .map(SendCourseStaffSmsRequest.Recipient::userId)
                .distinct()
                .toList();
        Map<Long, UsersEntity> userById = usersRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(UsersEntity::getUserId, Function.identity()));

        LocalDateTime now = LocalDateTime.now();
        int successCount = 0;
        int failedCount = 0;
        boolean anyLms = false;
        List<SendCourseStaffSmsResponse.Skipped> skipped = new ArrayList<>();

        for (SendCourseStaffSmsRequest.Group group : request.groups()) {
            // 그룹별 발송 대상 구성 — 전화번호 없는 인원은 skip(발송·이력 저장 제외).
            List<StaffTarget> targets = new ArrayList<>();
            int maxBytes = 0;
            for (SendCourseStaffSmsRequest.Recipient r : group.recipients()) {
                UsersEntity user = userById.get(r.userId());
                String name = user == null ? null : user.getName();
                String phone = StringUtils.hasText(r.phoneOverride())
                        ? r.phoneOverride().trim()
                        : (user == null ? null : user.getPhone());
                if (!StringUtils.hasText(phone)) {
                    skipped.add(new SendCourseStaffSmsResponse.Skipped(r.userId(), name));
                    continue;
                }
                String roleLabel = roleLabelByUser.getOrDefault(r.userId(), "");
                String content = substitute(group.content(), region, round, startDate, roleLabel);
                if (SmsByteCalculator.byteLength(content) > SmsByteCalculator.LMS_MAX_BYTES) {
                    throw new BusinessException(ErrorCode.SMS_CONTENT_TOO_LONG);
                }
                maxBytes = Math.max(maxBytes, SmsByteCalculator.byteLength(content));
                targets.add(new StaffTarget(r.userId(), phone, content));
            }
            if (targets.isEmpty()) {
                continue;
            }
            // 그룹 단위로 SMS(≤90)/LMS 판별 — 짧은 제외문/긴 배정문이 각자 형식을 갖는다.
            String format = SmsByteCalculator.resolveTextFormat(maxBytes);
            anyLms = anyLms || "LMS".equals(format);

            // SENS 한도(100건) → 100건 단위 분할 발송
            for (int start = 0; start < targets.size(); start += BATCH_SIZE) {
                List<StaffTarget> batch = targets.subList(start, Math.min(start + BATCH_SIZE, targets.size()));
                StaffSmsSendStatus status = sendBatch(format, group.content(), batch);
                for (StaffTarget t : batch) {
                    courseStaffSmsRepository.save(CourseStaffSmsEntity.builder()
                            .courseId(request.courseId())
                            .userId(t.userId())
                            .sentBy(sentBy)
                            .notifyType(group.notifyType())
                            .content(t.content())
                            .sendStatus(status)
                            .sentAt(now)
                            .createdAt(now)
                            .build());
                    if (status == StaffSmsSendStatus.FAIL) {
                        failedCount++;
                    } else {
                        successCount++;
                    }
                }
            }
        }

        int totalCount = successCount + failedCount;
        return new SendCourseStaffSmsResponse(
                anyLms ? "LMS" : "SMS", totalCount, successCount, failedCount, skipped);
    }

    // 한 배치를 발송하고 상태(SUCCESS/FAIL)를 돌려준다. 담당자 SMS 는 SUCCESS/FAIL 2상태(폴링 없음).
    private StaffSmsSendStatus sendBatch(String format, String templateContent, List<StaffTarget> batch) {
        List<SmsSendCommand.Recipient> commandRecipients = batch.stream()
                .map(t -> new SmsSendCommand.Recipient(t.phone(), t.content()))
                .toList();
        try {
            SmsSendResult result = smsService.send(new SmsSendCommand(
                    format, null, templateContent, commandRecipients, null, null, null));
            return result.success() ? StaffSmsSendStatus.SUCCESS : StaffSmsSendStatus.FAIL;
        } catch (BusinessException e) {
            // 발송 실패(SMS_SEND_FAILED)만 배치 FAIL 로 기록(트랜잭션 유지). 그 외 입력 오류는 전파.
            if (e.getErrorCode() != ErrorCode.SMS_SEND_FAILED) {
                throw e;
            }
            log.warn("[StaffSMS] 배치 발송 실패 — FAIL 로 기록. size={}", batch.size(), e);
            return StaffSmsSendStatus.FAIL;
        }
    }

    // 수신자별 치환: {region}=지역명, {round}=회차, {startDate}=개강일(M/d), {role}=배정 역할 라벨. 값 없으면 빈 문자열.
    private String substitute(String content, String region, Integer round, String startDate, String roleLabel) {
        return content
                .replace(REGION_PLACEHOLDER, region == null ? "" : region)
                .replace(ROUND_PLACEHOLDER, round == null ? "" : String.valueOf(round))
                .replace(START_DATE_PLACEHOLDER, startDate == null ? "" : startDate)
                .replace(ROLE_PLACEHOLDER, roleLabel == null ? "" : roleLabel);
    }

    private String roleLabel(StaffRole role) {
        if (role == null) {
            return "";
        }
        return switch (role) {
            case COUNSELOR -> "상담사";
            case LECTURER -> "강사";
            case STAFF -> "진행자";
            case PROJECT_MANAGER -> "PM";
            case PROJECT_LEADER -> "PL";
            case ADMIN_STAFF -> "행정인력";
        };
    }

    private record StaffTarget(Long userId, String phone, String content) {
    }

    @Override
    public CourseStaffSmsPageResponse findHistoryPage(
            Long effectiveSentBy,
            String notifyType,
            String sendStatus,
            Integer courseNumber,
            Integer localCourseNumber,
            Long regionId,
            Long parentRegionId,
            LocalDate sentDateFrom,
            LocalDate sentDateTo,
            String keyword,
            Integer page,
            Integer size) {
        Pageable pageable = PageRequest.of(sanitizePage(page), sanitizeSize(size), Sort.unsorted());
        LocalDateTime dateFrom = sentDateFrom == null ? null : sentDateFrom.atStartOfDay();
        LocalDateTime dateTo = sentDateTo == null ? null : sentDateTo.plusDays(1).atStartOfDay();

        List<Long> regionIds = regionResolver.resolveRegionIds(regionId, parentRegionId);
        if (regionIds != null && regionIds.isEmpty()) {
            return new CourseStaffSmsPageResponse(List.of(), sanitizePage(page), sanitizeSize(size), 0, 0);
        }

        Page<CourseStaffSmsEntity> result = courseStaffSmsRepository.findPageByFilters(
                effectiveSentBy,
                parseNotifyType(notifyType),
                parseSendStatus(sendStatus),
                courseNumber,
                localCourseNumber,
                regionIds,
                dateFrom,
                dateTo,
                normalize(keyword),
                pageable);

        List<CourseStaffSmsPageResponse.Item> content = result.getContent().stream()
                .map(this::toItem)
                .toList();
        return new CourseStaffSmsPageResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private CourseStaffSmsPageResponse.Item toItem(CourseStaffSmsEntity row) {
        var course = row.getCourse();
        return new CourseStaffSmsPageResponse.Item(
                row.getCourseStaffSmsId(),
                row.getCourseId(),
                course == null || course.getRegion() == null ? null : course.getRegion().getName(),
                course == null ? null : course.getCourseName(),
                course == null ? null : course.getCourseNumber(),
                course == null ? null : course.getLocalCourseNumber(),
                row.getUserId(),
                row.getRecipient() == null ? null : row.getRecipient().getName(),
                row.getRecipient() == null ? null : row.getRecipient().getPhone(),
                row.getSentBy(),
                row.getSender() == null ? null : row.getSender().getName(),
                row.getNotifyType() == null ? null : row.getNotifyType().name(),
                row.getContent(),
                row.getSendStatus() == null ? null : row.getSendStatus().name(),
                row.getSentAt());
    }

    private StaffNotifyType parseNotifyType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return StaffNotifyType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private StaffSmsSendStatus parseSendStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return StaffSmsSendStatus.valueOf(value.trim().toUpperCase());
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