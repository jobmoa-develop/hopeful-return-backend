package com.jobmoa.hopefulreturn.participantsms.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsDetailResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsListResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsPageResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ReservationCancelPreviewResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsRequest;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsResponse;
import com.jobmoa.hopefulreturn.participantsms.service.ParticipantSmsService;
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

    @Operation(summary = "문자 발송결과 재조회(수동)",
            description = "SENS 발송결과 조회로 상태·messageId·결과를 즉시 갱신 후 상세 반환. 권한: 문자 발송 권한(can_send_sms)")
    @PostMapping("/{smsId}/refresh")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<ParticipantSmsDetailResponse> refresh(@PathVariable Long smsId) {
        return ApiResponse.success(participantSmsService.refreshResult(smsId));
    }

    @Operation(summary = "문자 발송 내역 조회(전역·페이지)",
            description = "필터(수신자/상태/지역/회차/기간)+페이지네이션. "
                    + "ADMIN·HEAD_OFFICE 는 전체, 그 외 계정은 본인 발송분만. 권한: 문자 발송 권한(can_send_sms)")
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<ParticipantSmsPageResponse> history(
            @RequestAttribute("userId") Long userId,
            Authentication authentication,
            @Parameter(description = "수신자명/전화 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "발송 상태(SUCCESS/FAIL/PENDING/RESERVED/CANCELED)")
            @RequestParam(required = false) String sendStatus,
            @Parameter(description = "전체회차(course_number) — 전체 지역 조회 시 사용")
            @RequestParam(required = false) Integer courseNumber,
            @Parameter(description = "지역회차(local_course_number) — 지역 선택 조회 시 사용")
            @RequestParam(required = false) Integer localCourseNumber,
            @Parameter(description = "지역 ID(하위 지역)") @RequestParam(required = false) Long regionId,
            @Parameter(description = "상위 지역 ID(해당 상위지역의 모든 하위지역 포함 조회)")
            @RequestParam(required = false) Long parentRegionId,
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
        return ApiResponse.success(participantSmsService.findSmsHistoryPage(
                effectiveSentBy, sendStatus, courseNumber, localCourseNumber, regionId, parentRegionId,
                sentDateFrom, sentDateTo, keyword, page, size));
    }

    @Operation(summary = "예약 취소 사전 확인",
            description = "reserveId 로 함께 취소될(예약중) 대상 인원·수신자명을 반환. "
                    + "SENS 예약취소는 예약 batch(reserveId) 단위라 묶인 전원이 함께 취소된다. 권한: 문자 발송 권한(can_send_sms)")
    @GetMapping("/reservations/{reserveId}/cancel-preview")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<ReservationCancelPreviewResponse> cancelPreview(@PathVariable String reserveId) {
        return ApiResponse.success(participantSmsService.previewReservationCancel(reserveId));
    }

    @Operation(summary = "예약 발송 취소",
            description = "reserveId(예약 batch) 단위로 예약을 취소한다(묶인 전 수신자 동시 취소). 권한: 문자 발송 권한(can_send_sms)")
    @DeleteMapping("/reservations/{reserveId}")
    @PreAuthorize("hasAuthority('SMS_SEND')")
    public ApiResponse<Integer> cancelReservation(@PathVariable String reserveId) {
        return ApiResponse.success(participantSmsService.cancelReservation(reserveId));
    }
}
