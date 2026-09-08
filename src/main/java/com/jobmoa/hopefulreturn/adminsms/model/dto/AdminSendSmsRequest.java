package com.jobmoa.hopefulreturn.adminsms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "관리자 임의 번호 문자 발송 요청")
public record AdminSendSmsRequest(
        @Schema(description = "수신 전화번호 목록(하이픈 무관, 서버에서 정규화·검증·중복제거)",
                example = "[\"010-1234-5678\", \"01098765432\"]")
        @NotEmpty
        List<String> recipients,

        @Schema(description = "제목(LMS/MMS 전용, 최대 40바이트)", example = "안내")
        String title,

        @Schema(description = "본문 내용(모든 수신자 공통, 치환 없음)", example = "안녕하세요. 안내드립니다.")
        @NotBlank
        @Size(max = 2000)
        String content,

        @Schema(description = "발송 형식(SMS/LMS/MMS). 미지정 시 서버가 본문·이미지로 자동 판별", example = "LMS")
        String messageFormat,

        @Schema(description = "MMS 첨부 이미지(Base64, jpg/jpeg, 최대 300KB·1500x1440)")
        List<String> images,

        @Schema(description = "예약 발송 시각(yyyy-MM-dd HH:mm, Asia/Seoul). 미지정 시 즉시 발송", example = "2026-09-10 09:00")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}", message = "예약 시각 형식은 yyyy-MM-dd HH:mm 입니다.")
        String reserveTime
) {
}
