package com.jobmoa.hopefulreturn.followupcounsel.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantCounselorRepository;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.followupcounsel.entity.FollowUpCounselEntity;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.CreateFollowUpCounselRequest;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselCreatedResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselDeletedResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselDetailResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselListResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselUpdatedResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.UpdateFollowUpCounselRequest;
import com.jobmoa.hopefulreturn.followupcounsel.repository.FollowUpCounselRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowUpCounselServiceImpl implements FollowUpCounselService {

    /** 상담 방식(DB CHECK CK_FOLLOW_UP_COUNSEL_STATUS 와 동일 집합). 잘못된 값은 400으로 선차단. */
    private static final Set<String> COUNSEL_STATUSES = Set.of("landline", "text", "offline");

    private final FollowUpCounselRepository followUpCounselRepository;
    private final CourseParticipantRepository courseParticipantRepository;
    private final CourseParticipantCounselorRepository courseParticipantCounselorRepository;

    @Override
    public FollowUpCounselCreatedResponse create(CreateFollowUpCounselRequest request) {
        validateCourseParticipantExists(request.courseParticipantId());
        validateStatus(request.counselStatus());

        LocalDateTime now = LocalDateTime.now();
        FollowUpCounselEntity entity = FollowUpCounselEntity.builder()
                .courseParticipantId(request.courseParticipantId())
                .counselNumber(request.counselNumber())
                .counselDate(request.counselDate())
                .counselStatus(request.counselStatus())
                .counselMemo(request.counselMemo())
                .createdAt(now)
                .updatedAt(now)
                .build();

        FollowUpCounselEntity saved = followUpCounselRepository.save(entity);
        return new FollowUpCounselCreatedResponse(
                saved.getFollowUpCounselId(),
                saved.getCourseParticipantId(),
                saved.getCounselNumber(),
                saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUpCounselListResponse findAll(Long courseParticipantId, Long counselorScopeId) {
        validateCourseParticipantExists(courseParticipantId);
        verifyCounselorScope(courseParticipantId, counselorScopeId);
        List<FollowUpCounselListResponse.Item> content = followUpCounselRepository
                .findByCourseParticipantIdOrderByCounselNumberAsc(courseParticipantId)
                .stream()
                .map(this::toListItem)
                .toList();
        return new FollowUpCounselListResponse(content);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUpCounselDetailResponse findById(Long followUpCounselId, Long counselorScopeId) {
        FollowUpCounselEntity entity = findEntity(followUpCounselId);
        verifyCounselorScope(entity.getCourseParticipantId(), counselorScopeId);
        return toDetailResponse(entity);
    }

    @Override
    public FollowUpCounselUpdatedResponse update(Long followUpCounselId, UpdateFollowUpCounselRequest request) {
        validateStatus(request.counselStatus());
        FollowUpCounselEntity entity = findEntity(followUpCounselId);
        entity.setCounselDate(request.counselDate());
        entity.setCounselStatus(request.counselStatus());
        entity.setCounselMemo(request.counselMemo());
        entity.setUpdatedAt(LocalDateTime.now());
        followUpCounselRepository.save(entity);
        return new FollowUpCounselUpdatedResponse(entity.getFollowUpCounselId(), entity.getUpdatedAt());
    }

    @Override
    public FollowUpCounselDeletedResponse delete(Long followUpCounselId) {
        FollowUpCounselEntity entity = findEntity(followUpCounselId);
        followUpCounselRepository.delete(entity);
        return new FollowUpCounselDeletedResponse("사후관리 상담 정보가 삭제되었습니다.");
    }

    private void validateCourseParticipantExists(Long courseParticipantId) {
        if (!courseParticipantRepository.existsById(courseParticipantId)) {
            throw new BusinessException(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND);
        }
    }

    private void validateStatus(String status) {
        if (status != null && !COUNSEL_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * 상담사(COUNSELOR 전용) 스코프 가드 — counselorScopeId 가 있으면 그 상담사에게 배정된
     * 수강건만 조회 가능하다(FE 우회 불가). 관리자 등은 counselorScopeId 가 null 이라 통과한다.
     */
    private void verifyCounselorScope(Long courseParticipantId, Long counselorScopeId) {
        if (counselorScopeId == null) {
            return;
        }
        if (!courseParticipantCounselorRepository
                .existsByCourseParticipantIdAndCounselorId(courseParticipantId, counselorScopeId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private FollowUpCounselEntity findEntity(Long followUpCounselId) {
        return followUpCounselRepository.findById(followUpCounselId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLLOW_UP_COUNSEL_NOT_FOUND));
    }

    private FollowUpCounselListResponse.Item toListItem(FollowUpCounselEntity entity) {
        return new FollowUpCounselListResponse.Item(
                entity.getFollowUpCounselId(),
                entity.getCounselNumber(),
                entity.getCounselDate(),
                entity.getCounselStatus(),
                entity.getCounselMemo());
    }

    private FollowUpCounselDetailResponse toDetailResponse(FollowUpCounselEntity entity) {
        return new FollowUpCounselDetailResponse(
                entity.getFollowUpCounselId(),
                entity.getCourseParticipantId(),
                entity.getCounselNumber(),
                entity.getCounselDate(),
                entity.getCounselStatus(),
                entity.getCounselMemo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
