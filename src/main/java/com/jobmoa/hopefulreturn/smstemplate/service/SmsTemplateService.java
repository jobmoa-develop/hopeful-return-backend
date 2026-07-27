package com.jobmoa.hopefulreturn.smstemplate.service;

import com.jobmoa.hopefulreturn.smstemplate.model.dto.CreateSmsTemplateRequest;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateCreatedResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateDeletedResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateDetailResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateListResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateUpdatedResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.UpdateSmsTemplateRequest;

public interface SmsTemplateService {

    SmsTemplateCreatedResponse create(Long userId, CreateSmsTemplateRequest request);

    SmsTemplateListResponse findAll(Long userId);

    SmsTemplateDetailResponse findById(Long smsTemplateId, Long userId);

    SmsTemplateUpdatedResponse update(Long smsTemplateId, Long userId, UpdateSmsTemplateRequest request);

    SmsTemplateDeletedResponse delete(Long smsTemplateId, Long userId);
}
