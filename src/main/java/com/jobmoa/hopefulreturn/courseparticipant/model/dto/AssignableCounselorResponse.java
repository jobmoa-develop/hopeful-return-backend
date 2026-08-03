package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "배정 가능 상담사 목록 (해당 수강건 회차에 인력 배치된 상담사)")
public record AssignableCounselorResponse(
        @Schema(description = "상담사 목록")
        List<Item> counselors
) {
    @Schema(description = "배정 가능 상담사")
    public record Item(
            @Schema(description = "상담사(직원) ID", example = "12")
            Long counselorId,

            @Schema(description = "상담사 이름", example = "홍길동")
            String name
    ) {
    }
}
