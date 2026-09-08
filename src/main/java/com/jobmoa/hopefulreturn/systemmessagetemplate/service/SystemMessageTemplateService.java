package com.jobmoa.hopefulreturn.systemmessagetemplate.service;

import com.jobmoa.hopefulreturn.systemmessagetemplate.entity.SystemMessageTemplateKey;
import com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto.SystemMessageTemplateListResponse;
import com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto.SystemMessageTemplateUpdatedResponse;
import com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto.UpdateSystemMessageTemplateRequest;

public interface SystemMessageTemplateService {

    SystemMessageTemplateListResponse findAll();

    SystemMessageTemplateUpdatedResponse update(
            String templateKey, UpdateSystemMessageTemplateRequest request, Long updatedBy);

    /**
     * 발송 코드용 본문 조회. 행이 없거나 본문이 비면 {@code fallback}(기존 하드코딩 상수)을 반환해
     * 실발송이 절대 깨지지 않도록 한다.
     */
    String resolveContent(SystemMessageTemplateKey key, String fallback);
}
