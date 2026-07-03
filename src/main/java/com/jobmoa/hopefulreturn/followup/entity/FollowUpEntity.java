package com.jobmoa.hopefulreturn.followup.entity;

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
@Table(name = "follow_up")
public class FollowUpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "followup_id", nullable = false)
    private Long followupId;

    @Column(name = "course_participant_id", nullable = false)
    private Long courseParticipantId;

    @Column(name = "month_no")
    private Integer monthNo;

    @Column(name = "contact_date")
    private LocalDate contactDate;

    @Column(name = "contact_type", length = 30)
    private String contactType;

    @Column(name = "employment_status", length = 30)
    private String employmentStatus;

    @Column(name = "national_program", length = 30)
    private String nationalProgram;

    @Column(name = "forest_program")
    private Boolean forestProgram;

    @Column(name = "remark", length = 255)
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_participant_id", insertable = false, updatable = false)
    private CourseParticipantEntity courseParticipant;
}
