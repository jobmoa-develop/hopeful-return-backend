package com.jobmoa.hopefulreturn.courseopenreminder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 개강 하루전 자동 문자 발송 설정(단일 행). 발송 일자(며칠 전)·발송 시각·활성 여부를 보관한다.
 * {@code lastRunDate} 는 스케줄러가 하루 1회만 실행하도록 막는 가드로, 신규 스케줄러만 읽고 쓴다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_open_reminder_config")
public class CourseOpenReminderConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_config_id", nullable = false)
    private Long reminderConfigId;

    /** 자동 발송 on/off. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** 개강 며칠 전에 발송할지(1 = 하루 전). */
    @Column(name = "days_before", nullable = false)
    private int daysBefore;

    /** 발송 시각(스케줄러가 이 시각 이후에 하루 1회 실행). */
    @Column(name = "send_time", nullable = false)
    private LocalTime sendTime;

    /** 하루 1회 배치 실행 가드. 오늘 이미 실행했으면 today 로 채워진다. */
    @Column(name = "last_run_date")
    private LocalDate lastRunDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;
}
