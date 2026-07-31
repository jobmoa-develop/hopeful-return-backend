package com.jobmoa.hopefulreturn.courseparticipant.entity;

import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상담사/일정 변경 이력(append-only). 상담사 배정 변경·상담 일정 변경 시 변경 전·후 값과
 * 변경 주체(changed_by)·비고(reason)를 남긴다. 감사 로그 성격이라 별도 FK 제약은 두지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_participant_counselor_history")
public class CounselorChangeHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id", nullable = false)
    private Long historyId;

    /** 변경을 수행한 실제 계정(직원) — 로그인 사용자. */
    @Column(name = "account_user_id")
    private Long accountUserId;

    @Column(name = "course_participant_id", nullable = false)
    private Long courseParticipantId;

    /** 조회 편의용 역정규화 컬럼. */
    @Column(name = "course_number")
    private Integer courseNumber;

    @Column(name = "region_id")
    private Long regionId;

    /** 변경 대상 상담 슬롯. 일정 변경/상담사 변경 모두 해당 슬롯 기준. */
    @Enumerated(EnumType.STRING)
    @Column(name = "counseling_type", length = 30)
    private CounselingType counselingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private CounselorChangeType changeType;

    @Column(name = "old_counselor_id")
    private Long oldCounselorId;

    @Column(name = "new_counselor_id")
    private Long newCounselorId;

    @Column(name = "changed_date", nullable = false)
    private LocalDateTime changedDate;

    @Column(name = "old_started_at")
    private LocalDateTime oldStartedAt;

    @Column(name = "new_started_at")
    private LocalDateTime newStartedAt;

    @Column(name = "old_ended_at")
    private LocalDateTime oldEndedAt;

    @Column(name = "new_ended_at")
    private LocalDateTime newEndedAt;

    /** 변경 주체(빈칸/상담사/참여자). */
    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by", nullable = false, length = 20)
    private ChangeSubject changedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_counselor_id", insertable = false, updatable = false)
    private UsersEntity oldCounselor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_counselor_id", insertable = false, updatable = false)
    private UsersEntity newCounselor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_user_id", insertable = false, updatable = false)
    private UsersEntity accountUser;
}
