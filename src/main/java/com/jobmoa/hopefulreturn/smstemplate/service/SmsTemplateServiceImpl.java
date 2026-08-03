package com.jobmoa.hopefulreturn.smstemplate.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.smstemplate.entity.SmsTemplateEntity;
import com.jobmoa.hopefulreturn.smstemplate.entity.SmsTemplateScope;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.CreateSmsTemplateRequest;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateCreatedResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateDeletedResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateDetailResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateListResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateUpdatedResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.UpdateSmsTemplateRequest;
import com.jobmoa.hopefulreturn.smstemplate.repository.SmsTemplateRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class SmsTemplateServiceImpl implements SmsTemplateService {

    private final SmsTemplateRepository smsTemplateRepository;

    @Override
    public SmsTemplateCreatedResponse create(Long userId, CreateSmsTemplateRequest request) {
        SmsTemplateScope scope = parseScope(request.scope());
        LocalDateTime now = LocalDateTime.now();
        SmsTemplateEntity entity = SmsTemplateEntity.builder()
                // 개인 템플릿만 소유 계정을 지정한다(공용은 null).
                .userId(scope == SmsTemplateScope.PERSONAL ? userId : null)
                .scope(scope)
                .title(request.title())
                .content(request.content())
                .createdAt(now)
                .updatedAt(now)
                .build();

        SmsTemplateEntity saved = smsTemplateRepository.save(entity);
        return new SmsTemplateCreatedResponse(
                saved.getSmsTemplateId(),
                saved.getScope().name(),
                saved.getTitle(),
                saved.getContent(),
                saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public SmsTemplateListResponse findAll(Long userId) {
        List<SmsTemplateListResponse.Item> content = smsTemplateRepository
                .findVisibleTo(SmsTemplateScope.SHARED, userId)
                .stream()
                .map(this::toListItem)
                .toList();
        return new SmsTemplateListResponse(content);
    }

    @Override
    @Transactional(readOnly = true)
    public SmsTemplateDetailResponse findById(Long smsTemplateId, Long userId) {
        SmsTemplateEntity entity = findEntity(smsTemplateId);
        assertVisible(entity, userId);
        return new SmsTemplateDetailResponse(
                entity.getSmsTemplateId(),
                entity.getScope().name(),
                entity.getUserId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    @Override
    public SmsTemplateUpdatedResponse update(Long smsTemplateId, Long userId, UpdateSmsTemplateRequest request) {
        SmsTemplateEntity entity = findEntity(smsTemplateId);
        assertOwner(entity, userId);
        entity.setTitle(request.title());
        entity.setContent(request.content());
        entity.setUpdatedAt(LocalDateTime.now());
        smsTemplateRepository.save(entity);
        return new SmsTemplateUpdatedResponse(entity.getSmsTemplateId(), true);
    }

    @Override
    public SmsTemplateDeletedResponse delete(Long smsTemplateId, Long userId) {
        SmsTemplateEntity entity = findEntity(smsTemplateId);
        assertOwner(entity, userId);
        smsTemplateRepository.delete(entity);
        return new SmsTemplateDeletedResponse("문자 템플릿이 삭제되었습니다.");
    }

    private SmsTemplateListResponse.Item toListItem(SmsTemplateEntity entity) {
        return new SmsTemplateListResponse.Item(
                entity.getSmsTemplateId(),
                entity.getScope().name(),
                entity.getUserId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getUpdatedAt());
    }

    private SmsTemplateEntity findEntity(Long smsTemplateId) {
        return smsTemplateRepository.findById(smsTemplateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SMS_TEMPLATE_NOT_FOUND));
    }

    /**
     * 조회 가시성: 공용(SHARED)은 누구나, 개인(PERSONAL)은 소유 계정만.
     */
    private void assertVisible(SmsTemplateEntity entity, Long userId) {
        if (entity.getScope() == SmsTemplateScope.PERSONAL
                && (userId == null || !userId.equals(entity.getUserId()))) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * 수정·삭제 권한: 개인(PERSONAL)은 소유 계정만, 공용(SHARED)은 문자 권한 계정 공용.
     */
    private void assertOwner(SmsTemplateEntity entity, Long userId) {
        if (entity.getScope() == SmsTemplateScope.PERSONAL
                && (userId == null || !userId.equals(entity.getUserId()))) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private SmsTemplateScope parseScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        try {
            return SmsTemplateScope.valueOf(scope.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
