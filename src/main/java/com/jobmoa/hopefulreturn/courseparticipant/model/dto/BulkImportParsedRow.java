package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * 일괄 등록 엑셀 1행 파싱 결과. {@code error} 가 채워진 행은 등록에서 제외되고 리포트에만 표시된다.
 */
@Schema(description = "일괄 등록 파싱 행")
public record BulkImportParsedRow(
        @Schema(description = "엑셀 데이터 행 번호(1-based)", example = "1")
        int rowNumber,

        @Schema(description = "교육과정명(회차 그룹 키)", example = "[현장] (서울)리본(Re:Born)커리어_16회차")
        String sourceCourseName,

        @Schema(description = "시도", example = "서울특별시")
        String sido,

        @Schema(description = "시군구", example = "서울특별시 양천구")
        String sigungu,

        @Schema(description = "교육생명", example = "홍길동")
        String name,

        @Schema(description = "휴대폰번호(숫자만)", example = "01000000000")
        String phone,

        @Schema(description = "출생연도", example = "1986")
        Integer birthYear,

        @Schema(description = "신청일", example = "2026-07-09")
        LocalDate applyDate,

        @Schema(description = "선정일(접수/선정일시)", example = "2026-07-09")
        LocalDate receptionDate,

        @Schema(description = "선정여부 원문 — 선정/미선정/공란(엑셀 값 그대로)", example = "선정")
        String selected,

        @Schema(description = "등록 상태 — 선정→CONFIRMED, 미선정→CANCELED, 그 외→APPLIED", example = "CONFIRMED")
        String status,

        @Schema(description = "파싱/검증 오류 메시지(정상 시 null)", example = "휴대폰번호가 없습니다.")
        String error
) {
}
