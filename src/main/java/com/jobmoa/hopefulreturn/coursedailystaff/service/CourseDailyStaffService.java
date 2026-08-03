package com.jobmoa.hopefulreturn.coursedailystaff.service;

import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffCandidateResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffListResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.SaveCourseDailyStaffRequest;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.SaveCourseDailyStaffResponse;

public interface CourseDailyStaffService {

    CourseDailyStaffListResponse findAll(Long courseId);

    SaveCourseDailyStaffResponse save(SaveCourseDailyStaffRequest request);

    CourseDailyStaffCandidateResponse findCandidates(Long courseId);
}
