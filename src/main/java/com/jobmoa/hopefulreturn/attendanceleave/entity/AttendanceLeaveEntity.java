package com.jobmoa.hopefulreturn.attendanceleave.entity;

import com.jobmoa.hopefulreturn.attendance.entity.AttendanceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
@Table(name = "attendance_leave")
public class AttendanceLeaveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_leave_id", nullable = false)
    private Long attendanceLeaveId;

    @Column(name = "attendance_id", nullable = false)
    private Long attendanceId;

    @Column(name = "leave_time")
    private LocalTime leaveTime;

    @Column(name = "return_time")
    private LocalTime returnTime;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", insertable = false, updatable = false)
    private AttendanceEntity attendance;
}
