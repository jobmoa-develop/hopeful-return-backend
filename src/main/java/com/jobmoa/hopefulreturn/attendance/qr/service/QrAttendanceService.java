package com.jobmoa.hopefulreturn.attendance.qr.service;

import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrHistoryResponse;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrLandingResponse;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrLeaveRequest;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrLeaveReturnRequest;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrStatusResponse;
import com.jobmoa.hopefulreturn.attendance.qr.model.dto.QrVerifyRequest;

/**
 * QR 기반 참여자 공개 입·퇴실 서비스. 로그인 없이 성명+전화번호 뒤 4자리로 본인확인 후,
 * 현재 시각·강의 일정에 따라 입실/조퇴·외출/퇴실을 참여자 스스로 기록·조회한다.
 * 진행자(STAFF)용 {@code AttendanceService} 와는 분리해 공개 플로우의 상태 머신만 캡슐화한다.
 */
public interface QrAttendanceService {

    /** 랜딩(비-PII) — 본인확인 전 회차/오늘 일차/교육 시간창을 보여준다. */
    QrLandingResponse getLanding(Long courseId);

    /** 본인확인 후 오늘 출결 상태 + 가능 액션을 반환한다(기록 변경 없음). */
    QrStatusResponse verify(Long courseId, QrVerifyRequest request);

    /** 입실 — 10분 grace 초과 시 지각(LATE) + 자동 외출을 생성한다. */
    QrStatusResponse checkIn(Long courseId, QrVerifyRequest request);

    /** 조퇴(외출 시작) 기록. */
    QrStatusResponse leave(Long courseId, QrLeaveRequest request);

    /** 복귀(외출 종료) 기록. */
    QrStatusResponse leaveReturn(Long courseId, QrLeaveReturnRequest request);

    /** 퇴실 — 교육 종료 시각 이후에만 가능. */
    QrStatusResponse checkOut(Long courseId, QrVerifyRequest request);

    /** 본인 전 일차 입·퇴실 내역(읽기전용). */
    QrHistoryResponse history(Long courseId, QrVerifyRequest request);
}
