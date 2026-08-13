package com.jobmoa.hopefulreturn.coursestaffsms.model.dto;

import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffNotifyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "강좌 담당자 인력배정 안내 문자 발송 요청")
public record SendCourseStaffSmsRequest(
        @Schema(description = "대상 강좌 ID") @NotNull Long courseId,
        @Schema(description = "알림 종류별 발송 그룹(배정/변동/제외)") @NotEmpty @Valid List<Group> groups
) {

    @Schema(description = "알림 종류별 발송 그룹")
    public record Group(
            @Schema(description = "알림 종류(ASSIGN_NEW/ASSIGN_CHANGED/ASSIGN_REMOVED)") @NotNull StaffNotifyType notifyType,
            @Schema(description = "본문 템플릿({region}/{round}/{role} 토큰 치환)") @NotBlank String content,
            @Schema(description = "수신 담당자 목록") @NotEmpty @Valid List<Recipient> recipients
    ) {
    }

    @Schema(description = "수신 담당자")
    public record Recipient(
            @Schema(description = "담당자 user_id") @NotNull Long userId,
            @Schema(description = "이번 발송에만 적용할 전화번호(미지정 시 users.phone 사용)") String phoneOverride
    ) {
    }
}
