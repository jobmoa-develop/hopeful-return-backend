package com.jobmoa.hopefulreturn.courseopenreminder.service;

import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.CourseOpenReminderConfigResponse;
import com.jobmoa.hopefulreturn.courseopenreminder.model.dto.UpdateCourseOpenReminderConfigRequest;

public interface CourseOpenReminderConfigService {

    /** 현재 개강 하루전 자동 문자 발송 설정 조회. */
    CourseOpenReminderConfigResponse get();

    /** 설정 수정(활성 여부·며칠 전·발송 시각). */
    CourseOpenReminderConfigResponse update(UpdateCourseOpenReminderConfigRequest request, Long updatedBy);
}
