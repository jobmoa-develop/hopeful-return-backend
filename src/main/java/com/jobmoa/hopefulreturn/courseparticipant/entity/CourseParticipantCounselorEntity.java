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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_participant_counselor")
public class CourseParticipantCounselorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_participant_counselor_id", nullable = false)
    private Long courseParticipantCounselorId;

    @Column(name = "course_participant_id", nullable = false)
    private Long courseParticipantId;

    @Column(name = "counselor_id", nullable = false)
    private Long counselorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CounselingType status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "counseling_started_at")
    private LocalDateTime counselingStartedAt;

    @Column(name = "counseling_ended_at")
    private LocalDateTime counselingEndedAt;

    @Column(name = "counseling_memo", length = 1000)
    private String counselingMemo;

    /**
     * 상담 불가 여부 — true면 사유(시각·메모)와 무관하게 '상담 불가'로 표기·판정한다.
     */
    @Column(name = "unavailable", nullable = false)
    private boolean unavailable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", insertable = false, updatable = false)
    private UsersEntity counselor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_participant_id", insertable = false, updatable = false)
    private CourseParticipantEntity courseParticipant;

    /**
     * 상담 완료 여부 — 종료 일시가 입력되면 완료로 간주한다.
     */
    public boolean isCompleted() {
        return counselingEndedAt != null;
    }
}
