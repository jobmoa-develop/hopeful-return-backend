package com.jobmoa.hopefulreturn.course.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.course.model.dto.CourseDetailResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseStaffListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CreateCourseRequest;
import com.jobmoa.hopefulreturn.course.model.dto.CreateCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.DeleteCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseRequest;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseStatusRequest;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseStatusResponse;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import jakarta.persistence.criteria.Predicate;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseServiceImpl implements CourseService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final CourseRepository courseRepository;
    private final RegionRepository regionRepository;
    private final CourseParticipantRepository courseParticipantRepository;
    private final CourseStaffRepository courseStaffRepository;

    @Override
    public CreateCourseResponse create(CreateCourseRequest request, Long createdBy) {
        regionRepository.findById(request.regionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        CourseEntity course = CourseEntity.builder()
                .regionId(request.regionId())
                .courseNumber(request.courseNumber())
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
        return new CreateCourseResponse(savedCourse.getCourseId(), savedCourse.getStatus().name());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseListResponse findAll(Long regionId, String status, String keyword, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(
                sanitizePage(page),
                sanitizeSize(size),
                Sort.by(Sort.Direction.ASC, "courseId"));
        Page<CourseEntity> courses = courseRepository.findAll(
                buildSpecification(regionId, parseStatus(status), normalize(keyword)),
                pageable);
        List<CourseListResponse.Item> content = courses.getContent().stream()
                .map(this::toListItem)
                .toList();

        return new CourseListResponse(
                content,
                courses.getNumber(),
                courses.getSize(),
                courses.getTotalElements(),
                courses.getTotalPages());
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

        if (request.courseName() != null) {
            course.setCourseName(request.courseName());
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
        course.setUpdatedAt(LocalDateTime.now());

        courseRepository.save(course);
        return new UpdateCourseResponse(course.getCourseId(), true);
    }

    @Override
    public UpdateCourseStatusResponse updateStatus(Long courseId, UpdateCourseStatusRequest request) {
        CourseEntity course = findCourse(courseId);
        CourseStatus status = parseStatus(request.status());

        course.setStatus(status);
        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);

        return new UpdateCourseStatusResponse(course.getCourseId(), course.getStatus().name());
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
            Integer page,
            Integer size) {
        findCourse(courseId);

        Pageable pageable = PageRequest.of(
                sanitizePage(page),
                sanitizeSize(size),
                Sort.by(Sort.Direction.ASC, "courseParticipantId"));
        CourseParticipantStatus parsedStatus = parseParticipantStatus(status);
        String normalizedKeyword = normalize(keyword);
        List<CourseParticipantEntity> filteredParticipants = courseParticipantRepository.findByCourseId(courseId).stream()
                .filter(courseParticipant -> matchesStatus(courseParticipant, parsedStatus))
                .filter(courseParticipant -> matchesParticipantName(courseParticipant, normalizedKeyword))
                .toList();
        Page<CourseParticipantEntity> participants = toPage(filteredParticipants, pageable);
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

    private Specification<CourseEntity> buildSpecification(Long regionId, CourseStatus status, String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (regionId != null) {
                predicates.add(criteriaBuilder.equal(root.get("regionId"), regionId));
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

    private CourseListResponse.Item toListItem(CourseEntity course) {
        return new CourseListResponse.Item(
                course.getCourseId(),
                course.getCourseName(),
                course.getCourseNumber(),
                extractRegionName(course),
                course.getStatus() == null ? null : course.getStatus().name(),
                course.getCapacity(),
                courseParticipantRepository.findByCourseId(course.getCourseId()).size());
    }

    private CourseDetailResponse toDetailResponse(CourseEntity course) {
        return new CourseDetailResponse(
                course.getCourseId(),
                course.getRegionId(),
                extractRegionName(course),
                course.getCourseNumber(),
                course.getCourseName(),
                course.getStatus() == null ? null : course.getStatus().name(),
                course.getCapacity(),
                course.getMinimumCapacity(),
                course.getLocation(),
                course.getPlanSubmitDate());
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

    private boolean matchesStatus(CourseParticipantEntity courseParticipant, CourseParticipantStatus status) {
        return status == null || courseParticipant.getStatus() == status;
    }

    private boolean matchesParticipantName(CourseParticipantEntity courseParticipant, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        if (courseParticipant.getParticipant() == null
                || !StringUtils.hasText(courseParticipant.getParticipant().getName())) {
            return false;
        }
        return courseParticipant.getParticipant().getName().toLowerCase().contains(keyword.toLowerCase());
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
