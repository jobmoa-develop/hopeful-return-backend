package com.jobmoa.hopefulreturn.dashboard.repository;

import com.jobmoa.hopefulreturn.dashboard.entity.DashboardTaskCompletionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardTaskCompletionRepository extends JpaRepository<DashboardTaskCompletionEntity, Long> {

    List<DashboardTaskCompletionEntity> findAll();

    Optional<DashboardTaskCompletionEntity> findByTaskId(String taskId);

    void deleteByTaskId(String taskId);
}