package com.jobmoa.hopefulreturn.attendance.qr.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

/**
 * QR 랜딩(비-PII) 응답 — 참여자가 QR 을 스캔했을 때 본인확인 전에 보여줄 회차 정보.
 * 개인정보는 담지 않으며, 오늘이 교육일이 아니면 dayNo 는 null 이다.
 */
@Schema(description = "QR 공개 입·퇴실 랜딩 응답(비-PII)")
public record QrLandingResponse(
        @Schema(description = "회차 ID", example = "12")
        Long courseId,

        @Schema(description = "강좌명", example = "희망리턴 취업역량강화")
        String courseName,

        @Schema(description = "지역명", example = "서울 양천")
        String regionName,

        @Schema(description = "전체 회차 번호", example = "3")
        Integer courseNumber,

        @Schema(description = "지역 회차 번호", example = "1")
        Integer localCourseNumber,

        @Schema(description = "오늘 교육 일차(1~5). 교육일이 아니면 null", example = "1")
        Integer dayNo,

        @Schema(description = "교육 시작 시각", example = "09:00:00")
        LocalTime educationStartTime,

        @Schema(description = "교육 종료 시각", example = "18:00:00")
        LocalTime educationEndTime
) {
}
