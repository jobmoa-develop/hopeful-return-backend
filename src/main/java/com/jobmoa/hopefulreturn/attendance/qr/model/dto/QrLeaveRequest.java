package com.jobmoa.hopefulreturn.attendance.qr.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/**
 * QR 조퇴(외출 시작) 요청 — 본인확인(성명+뒤4자리) + 나간 시각(leaveTime).
 * attendance_leave.leave_time 만 기록되면 조퇴, 이후 복귀 시각이 채워지면 외출이 된다.
 */
@Schema(description = "QR 조퇴(외출 시작) 요청")
public record QrLeaveRequest(
        @Schema(description = "성명", example = "김철수")
        @NotBlank(message = "성명을 입력해주세요.")
        @Size(max = 50, message = "성명은 50자 이하로 입력해주세요.")
        String name,

        @Schema(description = "전화번호 뒤 4자리", example = "5678")
        @NotBlank(message = "전화번호 뒤 4자리를 입력해주세요.")
        @Pattern(regexp = "\\d{4}", message = "전화번호 뒤 4자리를 숫자로 입력해주세요.")
        String phoneLast4,

        @Schema(description = "외출(조퇴) 시각(미지정 시 서버 현재시각)", example = "14:30:00")
        LocalTime leaveTime
) {
}
