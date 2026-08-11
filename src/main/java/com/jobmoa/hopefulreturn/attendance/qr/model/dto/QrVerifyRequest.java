package com.jobmoa.hopefulreturn.attendance.qr.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * QR 공개 입·퇴실 본인확인 요청 — 성명 + 전화번호 뒤 4자리.
 * verify / check-in / check-out / history 등 본인확인만 필요한 액션에서 공용으로 쓴다.
 */
@Schema(description = "QR 본인확인 요청(성명 + 전화번호 뒤 4자리)")
public record QrVerifyRequest(
        @Schema(description = "성명", example = "김철수")
        @NotBlank(message = "성명을 입력해주세요.")
        @Size(max = 50, message = "성명은 50자 이하로 입력해주세요.")
        String name,

        @Schema(description = "전화번호 뒤 4자리", example = "5678")
        @NotBlank(message = "전화번호 뒤 4자리를 입력해주세요.")
        @Pattern(regexp = "\\d{4}", message = "전화번호 뒤 4자리를 숫자로 입력해주세요.")
        String phoneLast4
) {
}
