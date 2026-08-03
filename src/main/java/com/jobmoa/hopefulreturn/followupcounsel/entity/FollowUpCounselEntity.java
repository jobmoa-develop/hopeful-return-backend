package com.jobmoa.hopefulreturn.followupcounsel.entity;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사후관리 상담(follow_up_counsel) — 수강생 사후관리 단계의 상담 로그.
 * counsel_status 는 유선(landline)/문자(text)/대면(offline) 중 하나(DB CHECK).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "follow_up_counsel")
public class FollowUpCounselEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_up_counsel_id", nullable = false)
    private Long followUpCounselId;

    @Column(name = "course_participant_id", nullable = false)
    private Long courseParticipantId;

    @Column(name = "counsel_number")
    private Integer counselNumber;

    @Column(name = "counsel_date")
    private LocalDate counselDate;

    @Column(name = "counsel_status", length = 20)
    private String counselStatus;

    @Column(name = "counsel_memo", length = 255)
    private String counselMemo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_participant_id", insertable = false, updatable = false)
    private CourseParticipantEntity courseParticipant;
}
