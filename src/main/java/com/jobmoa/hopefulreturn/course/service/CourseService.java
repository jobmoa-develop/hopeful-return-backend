package com.jobmoa.hopefulreturn.course.service;

import com.jobmoa.hopefulreturn.course.model.dto.CourseListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseDetailResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CreateCourseRequest;
import com.jobmoa.hopefulreturn.course.model.dto.CreateCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.DeleteCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseStaffListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseRequest;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseStatusRequest;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseStatusResponse;
import com.jobmoa.hopefulreturn.course.scope.CourseScope;

public interface CourseService {

    CreateCourseResponse create(CreateCourseRequest request, Long createdBy);

    CourseListResponse findAll(
            Long regionId,
            Long parentRegionId,
            String status,
            String keyword,
            CourseScope scope,
            Integer page,
            Integer size);

    CourseDetailResponse findById(Long courseId);

    UpdateCourseResponse update(Long courseId, UpdateCourseRequest request);

    UpdateCourseStatusResponse updateStatus(Long courseId, UpdateCourseStatusRequest request);

    DeleteCourseResponse delete(Long courseId);

    CourseParticipantListResponse findParticipants(
            Long courseId,
            String status,
            String keyword,
            CourseScope scope,
            Integer page,
            Integer size);

    CourseStaffListResponse findStaffs(Long courseId);
}
