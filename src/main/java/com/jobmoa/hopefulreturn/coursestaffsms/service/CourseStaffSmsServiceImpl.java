package com.jobmoa.hopefulreturn.coursestaffsms.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.CourseStaffSmsEntity;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffNotifyType;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffSmsSendStatus;
import com.jobmoa.hopefulreturn.coursestaffsms.model.dto.CourseStaffSmsPageResponse;
import com.jobmoa.hopefulreturn.coursestaffsms.repository.CourseStaffSmsRepository;
import com.jobmoa.hopefulreturn.region.support.RegionResolver;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseStaffSmsServiceImpl implements CourseStaffSmsService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final CourseStaffSmsRepository courseStaffSmsRepository;
    private final RegionResolver regionResolver;

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