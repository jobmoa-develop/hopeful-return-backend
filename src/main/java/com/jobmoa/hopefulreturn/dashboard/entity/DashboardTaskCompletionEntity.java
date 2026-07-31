package com.jobmoa.hopefulreturn.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "dashboard_task_completion")
public class DashboardTaskCompletionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dashboard_task_completion_id", nullable = false)
    private Long dashboardTaskCompletionId;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}