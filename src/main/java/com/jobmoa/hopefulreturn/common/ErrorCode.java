package com.jobmoa.hopefulreturn.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "이메일 인증이 필요합니다."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "직원을 찾을 수 없습니다."),
    LOGIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 로그인 ID입니다."),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "역할을 찾을 수 없습니다."),
    INVALID_ROLE_NAME(HttpStatus.BAD_REQUEST, "유효하지 않은 역할명입니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여자를 찾을 수 없습니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "강좌를 찾을 수 없습니다."),
    COURSE_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 정보를 찾을 수 없습니다."),
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "출석 정보를 찾을 수 없습니다."),
    ATTENDANCE_LEAVE_NOT_FOUND(HttpStatus.NOT_FOUND, "조퇴·외출 정보를 찾을 수 없습니다."),
    PARTICIPANT_MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "상담 메모를 찾을 수 없습니다."),
    FOLLOW_UP_NOT_FOUND(HttpStatus.NOT_FOUND, "사후관리 정보를 찾을 수 없습니다."),
    STAFF_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "스태프 일정을 찾을 수 없습니다."),
    DUPLICATE_STAFF_SCHEDULE(HttpStatus.CONFLICT, "이미 등록된 스태프 일정입니다."),
    INVALID_SESSION_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 시간대(session_type)입니다."),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 상태값입니다."),
    COUNSELING_SLOT_DUPLICATED(HttpStatus.BAD_REQUEST, "같은 상담 구분을 중복 배정할 수 없습니다."),
    COUNSELING_SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 상담 구분에 배정된 상담사가 없습니다."),
    INVALID_COUNSELING_TIME(HttpStatus.BAD_REQUEST, "상담 종료 일시는 시작 일시 이후여야 합니다."),
    FORBIDDEN_COUNSELOR_ASSIGN(HttpStatus.FORBIDDEN, "직전 상담 단계의 상담사만 다음 상담사를 지정할 수 있습니다."),
    FORBIDDEN_COUNSELING_RECORD(HttpStatus.FORBIDDEN, "해당 상담에 배정된 상담사만 상담 기록을 입력할 수 있습니다."),
    COUNSELOR_NOT_ASSIGNABLE(HttpStatus.BAD_REQUEST, "해당 회차에 인력 배치된 상담사만 지정할 수 있습니다."),
    ASSIGN_CONFLICT(HttpStatus.CONFLICT, "다른 회차에 이미 배정된 인력이 있습니다."),
    ASSIGN_ON_UNAVAILABLE_DATE(HttpStatus.CONFLICT, "근무 불가일에는 배정할 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
