package com.jobmoa.hopefulreturn.participantsms.entity;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
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
@Table(name = "participant_sms")
public class ParticipantSmsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sms_id", nullable = false)
    private Long smsId;

    @Column(name = "sent_by")
    private Long sentBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "sms_type")
    private SmsType smsType;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "content")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status")
    private SendStatus sendStatus;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "course_participant_id", nullable = false)
    private Long courseParticipantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by", insertable = false, updatable = false)
    private UsersEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_participant_id", insertable = false, updatable = false)
    private CourseParticipantEntity courseParticipant;
}
