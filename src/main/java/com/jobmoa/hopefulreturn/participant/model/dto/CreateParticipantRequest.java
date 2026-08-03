package com.jobmoa.hopefulreturn.participant.model.dto;

import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorAssignment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "참여자 등록 요청 — enrollment가 있으면 수강(course_participant)도 한 트랜잭션으로 함께 등록")
public record CreateParticipantRequest(
        @Schema(description = "참여자명", example = "김철수")
        @NotBlank
        @Size(max = 50)
        String name,

        @Schema(description = "출생연도", example = "1978")
        Integer birthYear,

        @Schema(description = "전화번호", example = "010-5678-1234")
        @NotBlank
        @Size(max = 20)
        String phone,

        @Schema(description = "수강 등록 블록 — 지역·회차를 함께 선택한 경우에만 전달 (없으면 참여자만 생성)")
        @Valid
        Enrollment enrollment
) {

    @Schema(description = "참여자 등록 시 함께 생성할 수강 정보 (진행 상태는 CONFIRMED=선정으로 고정)")
    public record Enrollment(
            @Schema(description = "강좌 ID", example = "15")
            @NotNull
            Long courseId,

            @Schema(description = "유입 경로", example = "워크넷")
            @Size(max = 30)
            String inflowType,

            @Schema(description = "신청일", example = "2026-08-01")
            LocalDate applyDate,

            @Schema(description = "접수일", example = "2026-08-02")
            LocalDate receptionDate,

            @Schema(description = "기초교육 이수 여부", example = "Y")
            @Size(max = 20)
            String basicEducation,

            @Schema(description = "상담사 배정 목록 (사전/사후1/사후2 슬롯당 1명)")
            @Valid
            List<CounselorAssignment> counselors
    ) {
    }
}
