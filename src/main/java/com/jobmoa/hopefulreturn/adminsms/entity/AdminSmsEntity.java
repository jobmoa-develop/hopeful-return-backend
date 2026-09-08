package com.jobmoa.hopefulreturn.adminsms.entity;

import com.jobmoa.hopefulreturn.participantsms.entity.MessageFormat;
import com.jobmoa.hopefulreturn.participantsms.entity.SendStatus;
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
 * 관리자 임의 번호 문자 발송 이력(admin_sms, V29).
 * course_participant 에 묶이지 않는 원시 전화번호(to_phone)로 발송한 건을 저장한다.
 * 상태·형식 enum 은 참여자 SMS 와 동일 의미이므로 participantsms 의 {@link SendStatus}·{@link MessageFormat} 를 재사용한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "admin_sms")
public class AdminSmsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_sms_id", nullable = false)
    private Long adminSmsId;

    @Column(name = "sent_by")
    private Long sentBy;

    @Column(name = "to_phone", nullable = false, length = 20)
    private String toPhone;

    @Column(name = "recipient_label", length = 50)
    private String recipientLabel;

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

    // 발송결과 추적(V29). request_id=발송요청 단위, message_id=수신 건별(결과조회로 확보).
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

    // 예약 발송(V29). reserve_time=예약 발송 예정 시각, reserve_id=SENS 예약 취소 식별자(=requestId).
    @Column(name = "reserve_time")
    private LocalDateTime reserveTime;

    @Column(name = "reserve_id", length = 50)
    private String reserveId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by", insertable = false, updatable = false)
    private UsersEntity sender;
}
