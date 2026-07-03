package com.jobmoa.hopefulreturn.courseparticipant.entity;

import com.jobmoa.hopefulreturn.allowance.entity.AllowanceEntity;
import com.jobmoa.hopefulreturn.attendance.entity.AttendanceEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.followup.entity.FollowUpEntity;
import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import com.jobmoa.hopefulreturn.participantmemo.entity.ParticipantMemoEntity;
import com.jobmoa.hopefulreturn.participantsms.entity.ParticipantSmsEntity;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
@Table(name = "course_participant")
public class CourseParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_participant_id", nullable = false)
    private Long courseParticipantId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "participant_id", nullable = false)
    private Long participantId;

    @Column(name = "counselor_id")
    private Long counselorId;

    @Column(name = "inflow_type", length = 30)
    private String inflowType;

    @Column(name = "apply_date")
    private LocalDate applyDate;

    @Column(name = "reception_date")
    private LocalDate receptionDate;

    @Column(name = "basic_education", length = 20)
    private String basicEducation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CourseParticipantStatus status;

    @Column(name = "contact_attempt")
    private Integer contactAttempt;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "incomplete_reason", length = 255)
    private String incompleteReason;

    @Column(name = "allowance_paid")
    private Boolean allowancePaid;

    @Column(name = "allowance_date")
    private LocalDate allowanceDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private CourseEntity course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", insertable = false, updatable = false)
    private ParticipantEntity participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", insertable = false, updatable = false)
    private UsersEntity counselor;

    @OneToMany(mappedBy = "courseParticipant", fetch = FetchType.LAZY)
    private List<ParticipantMemoEntity> participantMemos;

    @OneToMany(mappedBy = "courseParticipant", fetch = FetchType.LAZY)
    private List<FollowUpEntity> followUps;

    @OneToMany(mappedBy = "courseParticipant", fetch = FetchType.LAZY)
    private List<ParticipantSmsEntity> participantSms;

    @OneToMany(mappedBy = "courseParticipant", fetch = FetchType.LAZY)
    private List<AllowanceEntity> allowances;

    @OneToMany(mappedBy = "courseParticipant", fetch = FetchType.LAZY)
    private List<AttendanceEntity> attendances;
}
