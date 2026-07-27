package com.jobmoa.hopefulreturn.smstemplate.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.CreateSmsTemplateRequest;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateCreatedResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateDeletedResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateDetailResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateListResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.SmsTemplateUpdatedResponse;
import com.jobmoa.hopefulreturn.smstemplate.model.dto.UpdateSmsTemplateRequest;
import com.jobmoa.hopefulreturn.smstemplate.service.SmsTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SmsTemplate")
@RestController
@RequestMapping("/api/sms-templates")
@RequiredArgsConstructor
public class SmsTemplateController {

    private final SmsTemplateService smsTemplateService;

    @Operation(summary = "문자 템플릿 등록", description = "권한: 문자 발송 권한(can_send_sms)")
    @PostMapping
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<SmsTemplateCreatedResponse> create(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CreateSmsTemplateRequest request) {
        return ApiResponse.success(smsTemplateService.create(userId, request));
    }

    @Operation(summary = "문자 템플릿 목록 조회", description = "공용 전체 + 본인 개인 템플릿")
    @GetMapping
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<SmsTemplateListResponse> findAll(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(smsTemplateService.findAll(userId));
    }

    @Operation(summary = "문자 템플릿 상세 조회")
    @GetMapping("/{smsTemplateId}")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<SmsTemplateDetailResponse> findById(
            @PathVariable Long smsTemplateId,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(smsTemplateService.findById(smsTemplateId, userId));
    }

    @Operation(summary = "문자 템플릿 수정")
    @PutMapping("/{smsTemplateId}")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<SmsTemplateUpdatedResponse> update(
            @PathVariable Long smsTemplateId,
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateSmsTemplateRequest request) {
        return ApiResponse.success(smsTemplateService.update(smsTemplateId, userId, request));
    }

    @Operation(summary = "문자 템플릿 삭제")
    @DeleteMapping("/{smsTemplateId}")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<SmsTemplateDeletedResponse> delete(
            @PathVariable Long smsTemplateId,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(smsTemplateService.delete(smsTemplateId, userId));
    }
}
