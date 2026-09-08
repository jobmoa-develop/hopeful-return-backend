package com.jobmoa.hopefulreturn.systemmessagetemplate.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto.SystemMessageTemplateListResponse;
import com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto.SystemMessageTemplateUpdatedResponse;
import com.jobmoa.hopefulreturn.systemmessagetemplate.model.dto.UpdateSystemMessageTemplateRequest;
import com.jobmoa.hopefulreturn.systemmessagetemplate.service.SystemMessageTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SystemMessageTemplate")
@RestController
@RequestMapping("/api/system-message-templates")
@RequiredArgsConstructor
public class SystemMessageTemplateController {

    private final SystemMessageTemplateService systemMessageTemplateService;

    @Operation(summary = "시스템 문자 템플릿 목록 조회",
            description = "배정·개강문자·인증코드 등 코드에 하드코딩됐던 발송 문구. 권한: ADMIN, HEAD_OFFICE")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE')")
    public ApiResponse<SystemMessageTemplateListResponse> findAll() {
        return ApiResponse.success(systemMessageTemplateService.findAll());
    }

    @Operation(summary = "시스템 문자 템플릿 수정",
            description = "본문(content)만 수정한다(key·표시명 고정, 생성·삭제 없음). 권한: ADMIN, HEAD_OFFICE")
    @PutMapping("/{templateKey}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE')")
    public ApiResponse<SystemMessageTemplateUpdatedResponse> update(
            @PathVariable String templateKey,
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateSystemMessageTemplateRequest request) {
        return ApiResponse.success(systemMessageTemplateService.update(templateKey, request, userId));
    }
}
