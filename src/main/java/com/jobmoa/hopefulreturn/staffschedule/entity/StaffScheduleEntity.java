package com.jobmoa.hopefulreturn.staffschedule.entity;

import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
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
 * 스태프 가용 일정. 특정 강좌에 종속되지 않는 사용자(users) 기준의 일반 가용 일정 풀이다.
 * (user_id, schedule_date, session_type) 조합이 UNIQUE 로 중복 등록을 막는다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "staff_schedule")
public class StaffScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_schedule_id", nullable = false)
    private Long staffScheduleId;

    // 등록 주체(스태프) — users.user_id 참조
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 참여 가능 날짜
    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    // 시간대: AM(오전)/PM(오후)/FULL(종일). coursestaff 의 SessionType 을 재사용한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private SessionType sessionType;

    // 가용 여부(true=가능). 향후 불가/휴가 확장 대비.
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    // 비고(사유 등)
    @Column(name = "memo")
    private String memo;

    // 배정 연결(nullable). NOT NULL = 회차 배정 행(연결 course_staff 의 회차·역할·session·직원).
    @Column(name = "course_staff_id")
    private Long courseStaffId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 목록에서 스태프명을 노출하기 위한 읽기 전용 연관(FK 컬럼은 위 userId 가 관리)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UsersEntity user;

    // 배정 행의 회차·역할·session 복원용 읽기 전용 연관(FK 컬럼은 위 courseStaffId 가 관리)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_staff_id", insertable = false, updatable = false)
    private CourseStaffEntity courseStaff;
}
