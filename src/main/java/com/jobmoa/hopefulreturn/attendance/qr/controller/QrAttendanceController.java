package com.jobmoa.hopefulreturn.attendance.qr.controller;

import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrHistoryResponse;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrLandingResponse;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrLeaveRequest;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrLeaveReturnRequest;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrStatusResponse;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrVerifyRequest;
import com.jobmoa.hopefulreturn.attendance.qr.service.QrAttendanceService;
import com.jobmoa.hopefulreturn.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * QR 기반 참여자 공개 입·퇴실 컨트롤러(로그인 불필요).
 * SecurityConfig 의 {@code /api/public/qr/**} 화이트리스트로 인증 없이 접근하며,
 * 본인확인(성명 + 전화번호 뒤 4자리)은 각 요청 본문에서 매번 수행한다.
 * 랜딩만 GET(비-PII)이고, 개인정보가 실리는 나머지는 URL·로그 노출을 피해 모두 POST 로 받는다.
 */
@Tag(name = "QrAttendance", description = "QR 공개 입·퇴실")
@RestController
@RequestMapping("/api/public/qr")
@RequiredArgsConstructor
public class QrAttendanceController {

    private final QrAttendanceService qrAttendanceService;

    @Operation(summary = "QR 랜딩 조회(비-PII)", description = "본인확인 전 회차/오늘 일차/교육 시간창을 반환한다.")
    @GetMapping("/courses/{courseId}")
    public ApiResponse<QrLandingResponse> getLanding(@PathVariable Long courseId) {
        return ApiResponse.success(qrAttendanceService.getLanding(courseId));
    }

    @Operation(summary = "본인확인 + 오늘 상태 조회", description = "성명 + 전화번호 뒤 4자리로 본인확인 후 오늘 출결 상태를 반환한다.")
    @PostMapping("/courses/{courseId}/verify")
    public ApiResponse<QrStatusResponse> verify(
            @PathVariable Long courseId, @Valid @RequestBody QrVerifyRequest request) {
        return ApiResponse.success(qrAttendanceService.verify(courseId, request));
    }

    @Operation(summary = "입실", description = "교육 시작 10분 이내면 출석, 초과면 지각 + 자동 외출을 기록한다.")
    @PostMapping("/courses/{courseId}/check-in")
    public ApiResponse<QrStatusResponse> checkIn(
            @PathVariable Long courseId, @Valid @RequestBody QrVerifyRequest request) {
        return ApiResponse.success(qrAttendanceService.checkIn(courseId, request));
    }

    @Operation(summary = "조퇴(외출 시작)", description = "나간 시각을 기록한다.")
    @PostMapping("/courses/{courseId}/leave")
    public ApiResponse<QrStatusResponse> leave(
            @PathVariable Long courseId, @Valid @RequestBody QrLeaveRequest request) {
        return ApiResponse.success(qrAttendanceService.leave(courseId, request));
    }

    @Operation(summary = "복귀(외출 종료)", description = "복귀 시각을 기록해 조퇴를 외출로 완성한다.")
    @PostMapping("/courses/{courseId}/leave/return")
    public ApiResponse<QrStatusResponse> leaveReturn(
            @PathVariable Long courseId, @Valid @RequestBody QrLeaveReturnRequest request) {
        return ApiResponse.success(qrAttendanceService.leaveReturn(courseId, request));
    }

    @Operation(summary = "퇴실", description = "교육 종료 시각 이후에만 퇴실을 기록한다.")
    @PostMapping("/courses/{courseId}/check-out")
    public ApiResponse<QrStatusResponse> checkOut(
            @PathVariable Long courseId, @Valid @RequestBody QrVerifyRequest request) {
        return ApiResponse.success(qrAttendanceService.checkOut(courseId, request));
    }

    @Operation(summary = "본인 입·퇴실 내역(읽기전용)", description = "회차 전 일차의 출결/조퇴·외출 내역을 반환한다.")
    @PostMapping("/courses/{courseId}/history")
    public ApiResponse<QrHistoryResponse> history(
            @PathVariable Long courseId, @Valid @RequestBody QrVerifyRequest request) {
        return ApiResponse.success(qrAttendanceService.history(courseId, request));
    }
}
