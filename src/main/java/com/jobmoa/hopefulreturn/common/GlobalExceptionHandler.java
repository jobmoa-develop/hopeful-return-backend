package com.jobmoa.hopefulreturn.common;

import com.jobmoa.hopefulreturn.coursedailystaff.exception.AssignConflictException;
import com.jobmoa.hopefulreturn.coursedailystaff.exception.AssignOnUnavailableDateException;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.AssignConflict;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.AssignUnavailable;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authorization.AuthorizationDeniedException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 배정 저장 중복 충돌 — 409 로 충돌 목록(data)과 함께 응답한다.
     * FE 는 data 의 충돌 목록으로 최종 확인 모달을 띄우고 confirmConflicts=true 로 재요청한다.
     */
    @ExceptionHandler(AssignConflictException.class)
    public ResponseEntity<ApiResponse<List<AssignConflict>>> handleAssignConflict(
            AssignConflictException e) {
        return ResponseEntity
                .status(ErrorCode.ASSIGN_CONFLICT.getStatus())
                .body(new ApiResponse<>(false, e.getConflicts(), ErrorCode.ASSIGN_CONFLICT.getMessage()));
    }

    /**
     * 근무 불가일 배정 거부 — 409 로 불가 목록(data)과 함께 응답한다.
     * 중복 충돌과 달리 override(confirmConflicts) 없는 하드 블록이므로, FE 는 목록의
     * 인력·날짜·시간대를 안내하고 사용자가 배정을 수정하도록 유도한다.
     */
    @ExceptionHandler(AssignOnUnavailableDateException.class)
    public ResponseEntity<ApiResponse<List<AssignUnavailable>>> handleAssignUnavailable(
            AssignOnUnavailableDateException e) {
        return ResponseEntity
                .status(ErrorCode.ASSIGN_ON_UNAVAILABLE_DATE.getStatus())
                .body(new ApiResponse<>(false, e.getUnavailable(),
                        ErrorCode.ASSIGN_ON_UNAVAILABLE_DATE.getMessage()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(
            AuthorizationDeniedException e) {

        return ResponseEntity
                .status(ErrorCode.ACCESS_DENIED.getStatus())
                .body(ApiResponse.error(ErrorCode.ACCESS_DENIED.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(code.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus()).body(ApiResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
