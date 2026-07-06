package com.jobmoa.hopefulreturn.coursestaff.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "강좌 담당자 목록 응답")
public record CourseStaffListResponse(
        List<Item> staffs
) {

    @Schema(description = "강좌 담당자 목록 항목")
    public record Item(
            @Schema(description = "강좌 담당자 ID", example = "8")
            Long courseStaffId,

            @Schema(description = "사용자 ID", example = "3")
            Long userId,

            @Schema(description = "이름", example = "홍길동")
            String name,

            @Schema(description = "담당 역할", example = "COUNSELOR")
            String staffRole,

            @Schema(description = "세션 유형", example = "FULL")
            String sessionType
    ) {
    }
}
