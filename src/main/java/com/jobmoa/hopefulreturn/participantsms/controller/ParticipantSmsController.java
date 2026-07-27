package com.jobmoa.hopefulreturn.participantsms.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsDetailResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsListResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsRequest;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsResponse;
import com.jobmoa.hopefulreturn.participantsms.service.ParticipantSmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ParticipantSms")
@RestController
@RequestMapping("/api/participant-sms")
@RequiredArgsConstructor
public class ParticipantSmsController {

    private final ParticipantSmsService participantSmsService;

    @Operation(summary = "문자 발송(일괄)", description = "권한: 문자 발송 권한(can_send_sms). {name}=성명 치환")
    @PostMapping
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<SendSmsResponse> send(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody SendSmsRequest request) {
        return ApiResponse.success(participantSmsService.send(userId, request));
    }

    @Operation(summary = "문자 발송 이력 조회", description = "수강생별 발송 이력")
    @GetMapping
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<ParticipantSmsListResponse> findByCourseParticipant(
            @Parameter(description = "수강생 ID") @RequestParam Long courseParticipantId) {
        return ApiResponse.success(participantSmsService.findByCourseParticipant(courseParticipantId));
    }

    @Operation(summary = "문자 발송 상세 조회")
    @GetMapping("/{smsId}")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<ParticipantSmsDetailResponse> findById(@PathVariable Long smsId) {
        return ApiResponse.success(participantSmsService.findById(smsId));
    }
}
