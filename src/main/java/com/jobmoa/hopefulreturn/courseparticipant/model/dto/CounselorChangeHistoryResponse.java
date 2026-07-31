package com.jobmoa.hopefulreturn.courseparticipant.model.dto;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselorChangeHistoryEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "상담사/일정 변경 이력 조회 응답")
public record CounselorChangeHistoryResponse(
        @Schema(description = "변경 이력 목록 (최신순)")
        List<Item> histories
) {

    public static CounselorChangeHistoryResponse from(List<CounselorChangeHistoryEntity> entities) {
        return new CounselorChangeHistoryResponse(entities.stream().map(Item::from).toList());
    }

    @Schema(description = "변경 이력 항목")
    public record Item(
            Long historyId,
            Long courseParticipantId,
            Integer courseNumber,
            Long regionId,
            @Schema(description = "상담 구분 — PRE_SESSION / POST_SESSION_1 / POST_SESSION_2")
            String counselingType,
            @Schema(description = "변경 종류 — COUNSELOR_CHANGE / SCHEDULE_CHANGE")
            String changeType,
            Long oldCounselorId,
            String oldCounselorName,
            Long newCounselorId,
            String newCounselorName,
            LocalDateTime changedDate,
            LocalDateTime oldStartedAt,
            LocalDateTime newStartedAt,
            LocalDateTime oldEndedAt,
            LocalDateTime newEndedAt,
            @Schema(description = "변경 주체 — NONE / COUNSELOR / PARTICIPANT")
            String changedBy,
            String reason,
            Long accountUserId,
            String accountUserName,
            LocalDateTime createdAt
    ) {
        public static Item from(CounselorChangeHistoryEntity e) {
            return new Item(
                    e.getHistoryId(),
                    e.getCourseParticipantId(),
                    e.getCourseNumber(),
                    e.getRegionId(),
                    e.getCounselingType() == null ? null : e.getCounselingType().name(),
                    e.getChangeType() == null ? null : e.getChangeType().name(),
                    e.getOldCounselorId(),
                    e.getOldCounselor() == null ? null : e.getOldCounselor().getName(),
                    e.getNewCounselorId(),
                    e.getNewCounselor() == null ? null : e.getNewCounselor().getName(),
                    e.getChangedDate(),
                    e.getOldStartedAt(),
                    e.getNewStartedAt(),
                    e.getOldEndedAt(),
                    e.getNewEndedAt(),
                    e.getChangedBy() == null ? null : e.getChangedBy().name(),
                    e.getReason(),
                    e.getAccountUserId(),
                    e.getAccountUser() == null ? null : e.getAccountUser().getName(),
                    e.getCreatedAt());
        }
    }
}
