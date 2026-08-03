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
import com.jobmoa.hopefulreturn.course.scope.CourseScope;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.region.repository.RegionRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime; // LocalTime import 추가
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.jobmoa.hopefulreturn.region.entity.RegionEntity;

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
            Integer page,
            Integer size) {
        Pageable pageable = PageRequest.of(
                sanitizePage(page),
                sanitizeSize(size),
                Sort.by(Sort.Direction.ASC, "courseId"));
        List<Long> regionIds = resolveRegionIds(regionId, parentRegionId);
        Page<CourseEntity> courses = courseRepository.findAll(
                buildSpecification(regionIds, parseStatus(status), normalize(keyword), scope),
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
    public UpdateCourseStatusResponse updateStatus(Long courseId, UpdateCourseStatusRequest request) {
        CourseEntity course = findCourse(courseId);
        CourseStatus status = parseStatus(request.status());
        validateEducationTimesForActiveStatus(course, status);

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

    private CourseListResponse.Item toListItem(CourseEntity course) {
        return new CourseListResponse.Item(
                course.getCourseId(),
                course.getCourseName(),
                course.getCourseNumber(),
                course.getLocalCourseNumber(),
                extractRegionName(course),
                course.getStatus() == null ? null : course.getStatus().name(),
                course.getCapacity(),
                courseParticipantRepository.findByCourseId(course.getCourseId()).size(),
                deriveYear(course.getDay1Date()),
                course.getDay1Date(),
                course.getDay2Date(),
                course.getDay3Date(),
                course.getDay4Date(),
                course.getDay5Date(),
                course.getBreakMinutes());
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
