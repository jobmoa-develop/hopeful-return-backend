package com.jobmoa.hopefulreturn.course.service;

import com.jobmoa.hopefulreturn.course.model.dto.CourseListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseDetailResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseStaffSmsHistoryResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CreateCourseRequest;
import com.jobmoa.hopefulreturn.course.model.dto.CreateCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.DeleteCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseStaffListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.NotifyCourseChangeRequest;
import com.jobmoa.hopefulreturn.course.model.dto.NotifyCourseChangeResponse;
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

    /**
     * 강좌 상태를 변경한다. 모집마감(CLOSED)·취소(CANCELED)로 새로 전환되면 담당자(PM 제외)에게
     * 안내 문자를 자동 발송하며, 이때 남는 발송 이력의 sentBy 에는 이 변경을 트리거한 계정(changedBy)이 기록된다.
     */
    UpdateCourseStatusResponse updateStatus(Long courseId, UpdateCourseStatusRequest request, Long changedBy);

    DeleteCourseResponse delete(Long courseId);

    CourseParticipantListResponse findParticipants(
            Long courseId,
            String status,
            String keyword,
            CourseScope scope,
            Integer page,
            Integer size);

    CourseStaffListResponse findStaffs(Long courseId);

    /**
     * 강좌 일정/장소(교육일·교육시간·휴게시간·교육장) 변경을 선택된 담당자(PM 제외)에게 문자로 안내한다.
     * 요청된 userId 중 이 회차에 실제 배치되지 않은 사용자나 PM은 무시된다(임의 대상 발송 방지).
     * sentBy: 이 발송을 트리거한 로그인 계정(감사 로그용, 없으면 null).
     */
    NotifyCourseChangeResponse notifyScheduleChange(Long courseId, NotifyCourseChangeRequest request, Long sentBy);

    /**
     * 강좌 담당자 대상 안내 문자(상태변경·일정변경) 발송 이력을 조회한다.
     */
    CourseStaffSmsHistoryResponse findStaffSmsHistory(Long courseId);
}