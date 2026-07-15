package com.jobmoa.hopefulreturn.courseparticipant.service;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ChangeCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ChangeCourseParticipantStatusRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CancelCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantStatusChangedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CompleteCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ContactAttemptResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselingSessionResponse;
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

public interface CourseParticipantService {

    CourseParticipantCreatedResponse create(CreateCourseParticipantRequest request);

    CourseParticipantCreatedResponse create(
            CreateCourseParticipantRequest request, CourseParticipantStatus initialStatus);

    CourseParticipantListResponse findAll(Long courseId, String status, String keyword, Integer page, Integer size);

    CourseParticipantDetailResponse findById(Long courseParticipantId);

    CourseParticipantUpdatedResponse update(Long courseParticipantId, UpdateCourseParticipantRequest request);

    CourseParticipantDeletedResponse delete(Long courseParticipantId);

    CourseParticipantCanceledResponse cancel(Long courseParticipantId, CancelCourseParticipantRequest request);

    CourseParticipantCompletionResponse complete(Long courseParticipantId, CompleteCourseParticipantRequest request);

    CourseParticipantStatusChangedResponse changeStatus(
            Long courseParticipantId, ChangeCourseParticipantStatusRequest request);

    ContactAttemptResponse increaseContactAttempt(Long courseParticipantId);

    CounselorChangedResponse changeCounselor(Long courseParticipantId, ChangeCounselorRequest request);

    CounselingSessionResponse recordCounselingSession(
            Long courseParticipantId, String counselingType, RecordCounselingSessionRequest request);
}
