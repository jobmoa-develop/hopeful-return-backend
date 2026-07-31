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

    @Enumerated(EnumType.STRING)
    @Column(name = "message_format", length = 10)
    private MessageFormat messageFormat;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "course_participant_id", nullable = false)
    private Long courseParticipantId;

    // 발송결과 추적(V15). request_id=발송요청 단위, message_id=수신 건별(결과조회로 확보).
    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(name = "message_id", length = 50)
    private String messageId;

    @Column(name = "result_code", length = 20)
    private String resultCode;

    @Column(name = "result_message", length = 200)
    private String resultMessage;

    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    // 예약 발송(V16). reserve_time=예약 발송 예정 시각, reserve_id=SENS 예약 취소 식별자(=requestId, 라이브 검증).
    @Column(name = "reserve_time")
    private LocalDateTime reserveTime;

    @Column(name = "reserve_id", length = 50)
    private String reserveId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by", insertable = false, updatable = false)
    private UsersEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_participant_id", insertable = false, updatable = false)
    private CourseParticipantEntity courseParticipant;
}
