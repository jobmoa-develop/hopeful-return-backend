package com.jobmoa.hopefulreturn.participantsms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "문자 발송 요청(일괄)")
public record SendSmsRequest(
        @Schema(description = "수신 대상 수강생 ID 목록", example = "[101, 102, 103]")
        @NotEmpty
        List<Long> courseParticipantIds,

        @Schema(description = "제목(LMS/MMS 전용, 최대 40바이트)", example = "수료 안내")
        String title,

        @Schema(description = "본문 내용 ({name} = 수신자 성명 치환)", example = "{name}님, 수료를 축하합니다.")
        @NotBlank
        @Size(max = 2000)
        String content,

        @Schema(description = "발송 형식(SMS/LMS/MMS). 미지정 시 서버가 본문·이미지로 자동 판별", example = "LMS")
        String messageFormat,

        @Schema(description = "MMS 첨부 이미지(Base64, jpg/jpeg, 최대 300KB·1500x1440)")
        List<String> images
) {
}
