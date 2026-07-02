package com.jobmoa.hopefulreturn.followup.repository;

import com.jobmoa.hopefulreturn.followup.entity.FollowUpEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowUpRepository extends JpaRepository<FollowUpEntity, Long> {

    List<FollowUpEntity> findByCourseParticipantId(Long courseParticipantId);

    List<FollowUpEntity> findByCourseParticipantIdAndMonthNo(Long courseParticipantId, Integer monthNo);
}
