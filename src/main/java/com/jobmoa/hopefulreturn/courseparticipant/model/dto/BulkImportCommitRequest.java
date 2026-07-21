package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 일괄 등록 커밋 요청 — 미리보기 후 운영자가 확인·수정한 행 목록을 그대로 전송한다.
 * 서버는 클라이언트 값을 신뢰하지 않고 행마다 재검증(전화 정규화·필수값·상태·회차 존재·중복)한다.
 */
@Schema(description = "일괄 등록 커밋 요청 (편집된 행 목록)")
public record BulkImportCommitRequest(
        @Schema(description = "등록할 행 목록")
        @NotNull
        List<Item> items
) {

    @Schema(description = "일괄 등록 커밋 행")
    public record Item(
            @Schema(description = "엑셀 데이터 행 번호(리포트용)", example = "1")
            int rowNumber,

            @Schema(description = "교육과정명(리포트용)", example = "[현장] (서울)리본(Re:Born)커리어_16회차")
            String sourceCourseName,

            @Schema(description = "등록할 내부 회차 courseId (null=건너뛰기)", example = "1")
            Long targetCourseId,

            @Schema(description = "교육생명", example = "홍길동")
            String name,

            @Schema(description = "휴대폰번호(서버에서 재정규화)", example = "01012345678")
            String phone,

            @Schema(description = "출생연도", example = "1986")
            Integer birthYear,

            @Schema(description = "신청일", example = "2026-07-09")
            LocalDate applyDate,

            @Schema(description = "선정일", example = "2026-07-09")
            LocalDate receptionDate,

            @Schema(description = "등록 상태 — APPLIED/CONFIRMED/CANCELED (미지정 시 APPLIED)", example = "CONFIRMED")
            String status
    ) {
    }
}
