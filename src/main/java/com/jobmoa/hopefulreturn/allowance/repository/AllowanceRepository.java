package com.jobmoa.hopefulreturn.allowance.repository;

import com.jobmoa.hopefulreturn.allowance.entity.AllowanceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllowanceRepository extends JpaRepository<AllowanceEntity, Long> {

    List<AllowanceEntity> findByCourseParticipantId(Long courseParticipantId);

    List<AllowanceEntity> findByPaid(Boolean paid);
}
