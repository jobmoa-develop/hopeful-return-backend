package com.jobmoa.hopefulreturn.participantmemo.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.courseparticipant.repository.CourseParticipantRepository;
import com.jobmoa.hopefulreturn.participantmemo.entity.ParticipantMemoEntity;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.CreateParticipantMemoRequest;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoCreatedResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoDeletedResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoDetailResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoListResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoUpdatedResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.UpdateParticipantMemoRequest;
import com.jobmoa.hopefulreturn.participantmemo.repository.ParticipantMemoRepository;
import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import com.jobmoa.hopefulreturn.users.repository.UsersRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantMemoServiceImpl implements ParticipantMemoService {

    private final ParticipantMemoRepository participantMemoRepository;
    private final CourseParticipantRepository courseParticipantRepository;
    private final UsersRepository usersRepository;

    @Override
    public ParticipantMemoCreatedResponse create(Long userId, CreateParticipantMemoRequest request) {
        validateCourseParticipantExists(request.courseParticipantId());
        UsersEntity user = findUser(userId);

        ParticipantMemoEntity entity = ParticipantMemoEntity.builder()
                .courseParticipantId(request.courseParticipantId())
                .userId(user.getUserId())
                .content(request.content())
                .createdAt(LocalDateTime.now())
                .build();

        ParticipantMemoEntity saved = participantMemoRepository.save(entity);
        return new ParticipantMemoCreatedResponse(
                saved.getMemoId(),
                saved.getCourseParticipantId(),
                user.getName(),
                saved.getContent(),
                saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantMemoListResponse findAll(Long courseParticipantId) {
        validateCourseParticipantExists(courseParticipantId);
        List<ParticipantMemoListResponse.Item> content = participantMemoRepository
                .findByCourseParticipantIdOrderByCreatedAtDesc(courseParticipantId)
                .stream()
                .map(this::toListItem)
                .toList();
        return new ParticipantMemoListResponse(content);
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantMemoDetailResponse findById(Long memoId) {
        ParticipantMemoEntity entity = findEntity(memoId);
        return new ParticipantMemoDetailResponse(
                entity.getMemoId(),
                entity.getCourseParticipantId(),
                entity.getUserId(),
                writerName(entity),
                entity.getContent(),
                entity.getCreatedAt());
    }

    @Override
    public ParticipantMemoUpdatedResponse update(Long memoId, UpdateParticipantMemoRequest request) {
        ParticipantMemoEntity entity = findEntity(memoId);
        entity.setContent(request.content());
        participantMemoRepository.save(entity);
        return new ParticipantMemoUpdatedResponse(entity.getMemoId(), true);
    }

    @Override
    public ParticipantMemoDeletedResponse delete(Long memoId) {
        ParticipantMemoEntity entity = findEntity(memoId);
        participantMemoRepository.delete(entity);
        return new ParticipantMemoDeletedResponse("상담 메모가 삭제되었습니다.");
    }

    private void validateCourseParticipantExists(Long courseParticipantId) {
        if (!courseParticipantRepository.existsById(courseParticipantId)) {
            throw new BusinessException(ErrorCode.COURSE_PARTICIPANT_NOT_FOUND);
        }
    }

    private UsersEntity findUser(Long userId) {
        return usersRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private ParticipantMemoEntity findEntity(Long memoId) {
        return participantMemoRepository.findById(memoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_MEMO_NOT_FOUND));
    }

    private ParticipantMemoListResponse.Item toListItem(ParticipantMemoEntity entity) {
        return new ParticipantMemoListResponse.Item(
                entity.getMemoId(),
                writerName(entity),
                entity.getContent(),
                entity.getCreatedAt());
    }

    private String writerName(ParticipantMemoEntity entity) {
        return entity.getUser() == null ? null : entity.getUser().getName();
    }
}
