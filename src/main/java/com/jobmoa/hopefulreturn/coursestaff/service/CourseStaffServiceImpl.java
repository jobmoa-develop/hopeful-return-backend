package com.jobmoa.hopefulreturn.coursestaff.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.course.repository.CourseRepository;
import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.coursestaff.entity.StaffRole;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.CourseStaffListResponse;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.CreateCourseStaffRequest;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.CreateCourseStaffResponse;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.DeleteCourseStaffResponse;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.UpdateCourseStaffRequest;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.UpdateCourseStaffResponse;
import com.jobmoa.hopefulreturn.coursestaff.repository.CourseStaffRepository;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseStaffServiceImpl implements CourseStaffService {

    private final CourseStaffRepository courseStaffRepository;
    private final CourseRepository courseRepository;
    private final UsersRepository usersRepository;

    @Override
    public CreateCourseStaffResponse create(CreateCourseStaffRequest request) {
        courseRepository.findById(request.courseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        usersRepository.findByUserIdAndDeletedFalse(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CourseStaffEntity courseStaff = CourseStaffEntity.builder()
                .courseId(request.courseId())
                .userId(request.userId())
                .staffRole(parseStaffRole(request.staffRole()))
                .sessionType(parseSessionType(request.sessionType()))
                .createdAt(LocalDateTime.now())
                .build();
        CourseStaffEntity savedCourseStaff = courseStaffRepository.save(courseStaff);

        return new CreateCourseStaffResponse(savedCourseStaff.getCourseStaffId());
    }

    @Override
    public UpdateCourseStaffResponse update(Long courseStaffId, UpdateCourseStaffRequest request) {
        CourseStaffEntity courseStaff = findCourseStaff(courseStaffId);
        courseStaff.setStaffRole(parseStaffRole(request.staffRole()));
        courseStaff.setSessionType(parseSessionType(request.sessionType()));
        courseStaffRepository.save(courseStaff);

        return new UpdateCourseStaffResponse(true);
    }

    @Override
    public DeleteCourseStaffResponse delete(Long courseStaffId) {
        CourseStaffEntity courseStaff = findCourseStaff(courseStaffId);
        courseStaffRepository.delete(courseStaff);
        return new DeleteCourseStaffResponse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseStaffListResponse findAll(Long courseId) {
        List<CourseStaffListResponse.Item> staffs = courseStaffRepository
                .findByCourseIdOrderByCourseStaffIdAsc(courseId).stream()
                .map(this::toListItem)
                .toList();
        return new CourseStaffListResponse(staffs);
    }

    private CourseStaffEntity findCourseStaff(Long courseStaffId) {
        return courseStaffRepository.findById(courseStaffId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    }

    private CourseStaffListResponse.Item toListItem(CourseStaffEntity courseStaff) {
        return new CourseStaffListResponse.Item(
                courseStaff.getCourseStaffId(),
                courseStaff.getUserId(),
                courseStaff.getUser() == null ? null : courseStaff.getUser().getName(),
                courseStaff.getStaffRole() == null ? null : courseStaff.getStaffRole().name(),
                courseStaff.getSessionType() == null ? null : courseStaff.getSessionType().name());
    }

    private StaffRole parseStaffRole(String staffRole) {
        try {
            return StaffRole.valueOf(staffRole.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private SessionType parseSessionType(String sessionType) {
        try {
            return SessionType.valueOf(sessionType.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
