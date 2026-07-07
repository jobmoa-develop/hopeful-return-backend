package com.jobmoa.hopefulreturn.followup.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.followup.entity.FollowUpEntity;
import com.jobmoa.hopefulreturn.followup.model.dto.CreateFollowUpRequest;
import com.jobmoa.hopefulreturn.followup.model.dto.CreateFollowUpResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.DeleteFollowUpResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpDetailResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpListResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.UpdateFollowUpRequest;
import com.jobmoa.hopefulreturn.followup.model.dto.UpdateFollowUpResponse;
import com.jobmoa.hopefulreturn.followup.repository.FollowUpRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowUpServiceImpl implements FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final CourseParticipantRepository courseParticipantRepository;

    @Override
    public CreateFollowUpResponse create(CreateFollowUpRequest request) {
        validateCourseParticipantExists(request.courseParticipantId());

        FollowUpEntity entity = FollowUpEntity.builder()
                .courseParticipantId(request.courseParticipantId())
                .monthNo(request.monthNo())
                .contactDate(request.contactDate())
                .contactType(request.contactType())
                .employmentStatus(request.employmentStatus())
                .nationalProgram(request.nationalProgram())
                .forestProgram(request.forestProgram())
                .remark(request.remark())
                .build();

        FollowUpEntity saved = followUpRepository.save(entity);
        return new CreateFollowUpResponse(
                saved.getFollowupId(),
                saved.getCourseParticipantId(),
                saved.getMonthNo());
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUpListResponse findAll(Long courseParticipantId) {
        validateCourseParticipantExists(courseParticipantId);
        List<FollowUpListResponse.Item> content = followUpRepository
                .findByCourseParticipantIdOrderByMonthNoAsc(courseParticipantId)
                .stream()
                .map(this::toListItem)
                .toList();
        return new FollowUpListResponse(content);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUpDetailResponse findById(Long followUpId) {
        FollowUpEntity entity = findEntity(followUpId);
        return toDetailResponse(entity);
    }

    @Override
    public UpdateFollowUpResponse update(Long followUpId, UpdateFollowUpRequest request) {
        FollowUpEntity entity = findEntity(followUpId);
        entity.setContactDate(request.contactDate());
        entity.setContactType(request.contactType());
        entity.setEmploymentStatus(request.employmentStatus());
        entity.setNationalProgram(request.nationalProgram());
        entity.setForestProgram(request.forestProgram());
        entity.setRemark(request.remark());
        followUpRepository.save(entity);
        return new UpdateFollowUpResponse(entity.getFollowupId(), true);
    }

    @Override
    public DeleteFollowUpResponse delete(Long followUpId) {
        FollowUpEntity entity = findEntity(followUpId);
        followUpRepository.delete(entity);
        return new DeleteFollowUpResponse("사후관리 정보가 삭제되었습니다.");
    }

    private void validateCourseParticipantExists(Long courseParticipantId) {
        if (!courseParticipantRepository.existsById(courseParticipantId)) {
            throw new BusinessException(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND);
        }
    }

    private FollowUpEntity findEntity(Long followUpId) {
        return followUpRepository.findById(followUpId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLLOW_UP_NOT_FOUND));
    }

    private FollowUpListResponse.Item toListItem(FollowUpEntity entity) {
        return new FollowUpListResponse.Item(
                entity.getFollowupId(),
                entity.getMonthNo(),
                entity.getContactDate(),
                entity.getEmploymentStatus(),
                entity.getContactType());
    }

    private FollowUpDetailResponse toDetailResponse(FollowUpEntity entity) {
        return new FollowUpDetailResponse(
                entity.getFollowupId(),
                entity.getCourseParticipantId(),
                entity.getMonthNo(),
                entity.getContactDate(),
                entity.getContactType(),
                entity.getEmploymentStatus(),
                entity.getNationalProgram(),
                entity.getForestProgram(),
                entity.getRemark());
    }
}
