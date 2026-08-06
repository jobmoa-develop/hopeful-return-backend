package com.jobmoa.hopefulreturn.courseparticipant.service;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.AssignSlotCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.AssignableCounselorResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkAssignCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkCompleteCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkCompletionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkCounselorAssignResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ChangeCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ChangeCourseParticipantStatusRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CancelCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantStatusChangedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CompleteCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ContactAttemptResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselingSessionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorChangeHistoryResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorChangedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.RecordCounselingSessionRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCanceledResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCompletionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantDetailResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantUpdatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CreateCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.UpdateCourseParticipantRequest;

import java.util.List;
import java.util.Set;

public interface CourseParticipantService {

    CourseParticipantCreatedResponse create(CreateCourseParticipantRequest request);

    CourseParticipantCreatedResponse create(
            CreateCourseParticipantRequest request, CourseParticipantStatus initialStatus);

    CourseParticipantListResponse findAll(
            Long courseId,
            Long regionId,
            Long parentRegionId,
            Integer courseNumber,
            Integer localCourseNumber,
            String status,
            String keyword,
            Set<Long> allowedCourseParticipantIds,
            java.time.LocalDate registerDateFrom,
            java.time.LocalDate registerDateTo,
            Integer page,
            Integer size);

    CourseParticipantDetailResponse findById(Long courseParticipantId, Set<Long> allowedCourseParticipantIds);

    CourseParticipantUpdatedResponse update(Long courseParticipantId, UpdateCourseParticipantRequest request);

    CourseParticipantDeletedResponse delete(Long courseParticipantId);

    CourseParticipantCanceledResponse cancel(Long courseParticipantId, CancelCourseParticipantRequest request);

    CourseParticipantCompletionResponse complete(Long courseParticipantId, CompleteCourseParticipantRequest request);

    BulkCompletionResponse bulkComplete(BulkCompleteCourseParticipantRequest request);

    BulkCounselorAssignResponse bulkAssignCounselor(BulkAssignCounselorRequest request);

    AssignableCounselorResponse findAssignableCounselors(Long courseParticipantId);

    CounselorChangedResponse assignSlotCounselor(
            Long courseParticipantId,
            String counselingType,
            AssignSlotCounselorRequest request,
            Long requesterUserId,
            boolean requesterIsCounselorOnly);

    CourseParticipantStatusChangedResponse changeStatus(
            Long courseParticipantId, ChangeCourseParticipantStatusRequest request);

    ContactAttemptResponse increaseContactAttempt(Long courseParticipantId);

    CounselorChangedResponse changeCounselor(
            Long courseParticipantId, ChangeCounselorRequest request, Long actorUserId);

    /** 수강건의 상담사/일정 변경 이력을 최신순으로 조회한다. */
    CounselorChangeHistoryResponse getCounselorHistory(Long courseParticipantId);

    CounselingSessionResponse recordCounselingSession(
            Long courseParticipantId,
            String counselingType,
            RecordCounselingSessionRequest request,
            Long requesterUserId,
            boolean requesterIsCounselorOnly);


    /**
     * findAll과 동일한 필터(지역/회차/상태/키워드/스코프)를 적용하되, 페이지네이션(최대 100건 제한) 없이
     * 매칭되는 courseParticipantId 전체를 반환한다. 집계(카운트) 용도 전용.
     */
    List<Long> findAllIds(
            Long courseId,
            Long regionId,
            Long parentRegionId,
            Integer courseNumber,
            Integer localCourseNumber,
            String status,
            String keyword,
            Set<Long> allowedCourseParticipantIds,
            java.time.LocalDate registerDateFrom,
            java.time.LocalDate registerDateTo);
}
