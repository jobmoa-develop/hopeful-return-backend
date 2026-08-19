package com.jobmoa.hopefulreturn.staffunavailablenotice.entity;

import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 인력 근무불가 전환(가능→불가) 메일 알림 발송 이력. 수신자 1명당 1행.
 * 배정된 회차의 날짜(staff_schedule.course_staff_id NOT NULL)를 불가로 바꿨을 때만 기록된다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "staff_unavailable_notice")
public class StaffUnavailableNoticeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_unavailable_notice_id", nullable = false)
    private Long staffUnavailableNoticeId;

    // 전환된 staff_schedule 행
    @Column(name = "staff_schedule_id", nullable = false)
    private Long staffScheduleId;

    // 배정 연결(course_staff). 판정 근거(nullable — 방어적으로 null 허용)
    @Column(name = "course_staff_id")
    private Long courseStaffId;

    // 불가로 전환한 인력 → users
    @Column(name = "staff_user_id", nullable = false)
    private Long staffUserId;

    // 알림 수신 관리자 → users
    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    // 발송 시점 수신 이메일 스냅샷
    @Column(name = "recipient_email", length = 100)
    private String recipientEmail;

    // 불가로 바뀐 날짜
    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    // 시간대: AM/PM/FULL
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", length = 30)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", length = 50)
    private NoticeSendStatus sendStatus;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_user_id", insertable = false, updatable = false)
    private UsersEntity staffUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", insertable = false, updatable = false)
    private UsersEntity recipient;
}
