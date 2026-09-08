package com.jobmoa.hopefulreturn.adminsms.controller;

import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminReservationCancelPreviewResponse;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSendSmsRequest;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSendSmsResponse;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSmsDetailResponse;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSmsPageResponse;
import com.jobmoa.hopefulreturn.adminsms.service.AdminSmsService;
import com.jobmoa.hopefulreturn.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminSms")
@RestController
@RequestMapping("/api/admin-sms")
@RequiredArgsConstructor
public class AdminSmsController {

    private final AdminSmsService adminSmsService;

    @Operation(summary = "임의 번호 문자 발송", description = "관리자가 직접 입력한 전화번호로 발송. 권한: 문자 발송 권한(can_send_sms)")
    @PostMapping
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<AdminSendSmsResponse> send(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody AdminSendSmsRequest request) {
        return ApiResponse.success(adminSmsService.send(userId, request));
    }

    @Operation(summary = "임의 문자 발송 상세 조회")
    @GetMapping("/{adminSmsId}")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<AdminSmsDetailResponse> findById(@PathVariable Long adminSmsId) {
        return ApiResponse.success(adminSmsService.findById(adminSmsId));
    }

    @Operation(summary = "임의 문자 발송결과 재조회(수동)",
            description = "SENS 발송결과 조회로 상태·messageId·결과를 즉시 갱신 후 상세 반환. 권한: 문자 발송 권한(can_send_sms)")
    @PostMapping("/{adminSmsId}/refresh")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<AdminSmsDetailResponse> refresh(@PathVariable Long adminSmsId) {
        return ApiResponse.success(adminSmsService.refreshResult(adminSmsId));
    }

    @Operation(summary = "임의 문자 발송 내역 조회(전역·페이지)",
            description = "필터(수신번호·상태·기간)+페이지네이션. "
                    + "ADMIN·HEAD_OFFICE 는 전체, 그 외 계정은 본인 발송분만. 권한: 문자 발송 권한(can_send_sms)")
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<AdminSmsPageResponse> history(
            @RequestAttribute("userId") Long userId,
            Authentication authentication,
            @Parameter(description = "수신번호/라벨 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "발송 상태(SUCCESS/FAIL/PENDING/RESERVED/CANCELED)")
            @RequestParam(required = false) String sendStatus,
            @Parameter(description = "발송일 시작(YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sentDateFrom,
            @Parameter(description = "발송일 종료(YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sentDateTo,
            @Parameter(description = "페이지 번호(0-base)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기(≤100)") @RequestParam(required = false) Integer size) {
        // 역할 스코프: ADMIN·HEAD_OFFICE 는 전체(null), 그 외는 본인 발송분만. 서버에서만 결정한다.
        boolean seeAll = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_HEAD_OFFICE"));
        Long effectiveSentBy = seeAll ? null : userId;
        return ApiResponse.success(adminSmsService.findSmsHistoryPage(
                effectiveSentBy, sendStatus, sentDateFrom, sentDateTo, keyword, page, size));
    }

    @Operation(summary = "예약 취소 사전 확인",
            description = "reserveId 로 함께 취소될(예약중) 대상 인원·수신번호를 반환. "
                    + "SENS 예약취소는 예약 batch(reserveId) 단위라 묶인 전건이 함께 취소된다. 권한: 문자 발송 권한(can_send_sms)")
    @GetMapping("/reservations/{reserveId}/cancel-preview")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<AdminReservationCancelPreviewResponse> cancelPreview(@PathVariable String reserveId) {
        return ApiResponse.success(adminSmsService.previewReservationCancel(reserveId));
    }

    @Operation(summary = "예약 발송 취소",
            description = "reserveId(예약 batch) 단위로 예약을 취소한다(묶인 전 수신자 동시 취소). 권한: 문자 발송 권한(can_send_sms)")
    @DeleteMapping("/reservations/{reserveId}")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<Integer> cancelReservation(@PathVariable String reserveId) {
        return ApiResponse.success(adminSmsService.cancelReservation(reserveId));
    }
}
