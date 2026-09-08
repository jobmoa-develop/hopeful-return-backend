package com.jobmoa.hopefulreturn.systemmessagetemplate.service;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.systemmessagetemplate.entity.SystemMessageTemplateEntity;
import com.jobmoa.hopefulreturn.systemmessagetemplate.entity.SystemMessageTemplateKey;
import com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto.SystemMessageTemplateListResponse;
import com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto.SystemMessageTemplateUpdatedResponse;
import com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto.UpdateSystemMessageTemplateRequest;
import com.jobmoa.hopefulreturn.systemmessagetemplate.repository.SystemMessageTemplateRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemMessageTemplateServiceImpl implements SystemMessageTemplateService {

    private final SystemMessageTemplateRepository repository;

    @Override
    @Transactional(readOnly = true)
    public SystemMessageTemplateListResponse findAll() {
        List<SystemMessageTemplateListResponse.Item> content = repository
                .findAllByOrderByTemplateKeyAsc()
                .stream()
                .map(this::toListItem)
                .toList();
        return new SystemMessageTemplateListResponse(content);
    }

    @Override
    public SystemMessageTemplateUpdatedResponse update(
            String templateKey, UpdateSystemMessageTemplateRequest request, Long updatedBy) {
        SystemMessageTemplateEntity entity = repository.findByTemplateKey(templateKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYSTEM_TEMPLATE_NOT_FOUND));
        entity.setContent(request.content());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(updatedBy);
        repository.save(entity);
        return new SystemMessageTemplateUpdatedResponse(entity.getSystemMessageTemplateId(), true);
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveContent(SystemMessageTemplateKey key, String fallback) {
        return repository.findByTemplateKey(key.name())
                .map(SystemMessageTemplateEntity::getContent)
                .filter(StringUtils::hasText)
                .orElse(fallback);
    }

    private SystemMessageTemplateListResponse.Item toListItem(SystemMessageTemplateEntity entity) {
        return new SystemMessageTemplateListResponse.Item(
                entity.getSystemMessageTemplateId(),
                entity.getTemplateKey(),
                entity.getName(),
                entity.getDescription(),
                entity.getContent(),
                entity.getUpdatedAt());
    }
}
