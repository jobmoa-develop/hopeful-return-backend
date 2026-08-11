package com.jobmoa.hopefulreturn.attendance.qr.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/**
 * QR 복귀(외출 종료) 요청 — 본인확인(성명+뒤4자리) + 복귀 시각(returnTime).
 * attendanceLeaveId 를 주면 해당 조퇴 건에, 없으면 복귀 시각이 비어 있는 최신 건에 복귀 시각을 채운다.
 */
@Schema(description = "QR 복귀(외출 종료) 요청")
public record QrLeaveReturnRequest(
        @Schema(description = "성명", example = "김철수")
        @NotBlank(message = "성명을 입력해주세요.")
        @Size(max = 50, message = "성명은 50자 이하로 입력해주세요.")
        String name,

        @Schema(description = "전화번호 뒤 4자리", example = "5678")
        @NotBlank(message = "전화번호 뒤 4자리를 입력해주세요.")
        @Pattern(regexp = "\\d{4}", message = "전화번호 뒤 4자리를 숫자로 입력해주세요.")
        String phoneLast4,

        @Schema(description = "복귀시킬 조퇴·외출 ID(미지정 시 복귀 미기록 최신 건)", example = "5")
        Long attendanceLeaveId,

        @Schema(description = "복귀 시각(미지정 시 서버 현재시각)", example = "15:20:00")
        LocalTime returnTime
) {
}
