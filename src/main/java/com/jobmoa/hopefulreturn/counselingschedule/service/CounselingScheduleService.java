package com.jobmoa.hopefulreturn.counselingschedule.service;

import com.jobmoa.hopefulreturn.counselingschedule.model.dto.CounselingScheduleResponse;
import java.time.LocalDate;

public interface CounselingScheduleService {

    /**
     * 상담 시작일 기준 상담 일정을 조회한다(전 회차·전 상담사, 스코프 미적용).
     * from/to 는 포함 범위이며, null 이면 서비스에서 기본 범위(당월)로 대체한다.
     */
    CounselingScheduleResponse findSchedules(
            LocalDate from, LocalDate to,
            Long regionId, Long parentRegionId, Integer courseNumber, Integer localCourseNumber,
            String counselorName);
}
