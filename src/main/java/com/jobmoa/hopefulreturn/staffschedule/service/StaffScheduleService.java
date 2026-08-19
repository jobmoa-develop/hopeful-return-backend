package com.jobmoa.hopefulreturn.staffschedule.service;

import com.jobmoa.hopefulreturn.staffschedule.model.dto.BulkStaffScheduleRequest;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.BulkStaffScheduleResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.CreateStaffScheduleRequest;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.StaffScheduleDeletedResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.StaffScheduleListResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.StaffScheduleResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.UpdateStaffScheduleRequest;
import java.time.LocalDate;

/**
 * 스태프 가용 일정 서비스. {@code requesterId}·{@code isManager}는 컨트롤러가 인증 컨텍스트에서
 * 추출해 전달하며(서비스는 SecurityContext 비의존), 소유권 검증에 사용한다.
 */
public interface StaffScheduleService {

    StaffScheduleResponse create(Long requesterId, boolean isManager, CreateStaffScheduleRequest request);

    BulkStaffScheduleResponse createBulk(Long requesterId, boolean isManager, BulkStaffScheduleRequest request);

    StaffScheduleListResponse findAll(
            Long userId, LocalDate fromDate, LocalDate toDate, String sessionType, Integer page, Integer size);

    StaffScheduleListResponse findMy(Long requesterId, LocalDate fromDate, LocalDate toDate);

    StaffScheduleResponse findById(Long staffScheduleId);

    StaffScheduleResponse update(
            Long staffScheduleId, Long requesterId, boolean isManager, UpdateStaffScheduleRequest request);

    StaffScheduleDeletedResponse delete(Long staffScheduleId, Long requesterId, boolean isManager, String reason);
}
