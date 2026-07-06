package com.jobmoa.hopefulreturn.coursestaff.service;

import com.jobmoa.hopefulreturn.coursestaff.model.dto.CourseStaffListResponse;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.CreateCourseStaffRequest;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.CreateCourseStaffResponse;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.DeleteCourseStaffResponse;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.UpdateCourseStaffRequest;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.UpdateCourseStaffResponse;

public interface CourseStaffService {

    CreateCourseStaffResponse create(CreateCourseStaffRequest request);

    UpdateCourseStaffResponse update(Long courseStaffId, UpdateCourseStaffRequest request);

    DeleteCourseStaffResponse delete(Long courseStaffId);

    CourseStaffListResponse findAll(Long courseId);
}
