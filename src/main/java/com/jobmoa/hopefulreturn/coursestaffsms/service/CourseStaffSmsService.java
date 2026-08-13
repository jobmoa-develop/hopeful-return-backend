package com.jobmoa.hopefulreturn.coursestaffsms.service;

import com.jobmoa.hopefulreturn.coursestaffsms.model.dto.CourseStaffSmsPageResponse;
import com.jobmoa.hopefulreturn.coursestaffsms.model.dto.SendCourseStaffSmsRequest;
import com.jobmoa.hopefulreturn.coursestaffsms.model.dto.SendCourseStaffSmsResponse;
import java.time.LocalDate;

public interface CourseStaffSmsService {

    // 인력배정 안내 문자 발송(배정/변동/제외 그룹). sentBy=발송을 트리거한 로그인 계정.
    SendCourseStaffSmsResponse send(Long sentBy, SendCourseStaffSmsRequest request);

    // 전역 발송내역 조회(페이지·필터). effectiveSentBy=null 이면 전체, 값이 있으면 해당 발송자만.
    CourseStaffSmsPageResponse findHistoryPage(
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
            Integer size);
}