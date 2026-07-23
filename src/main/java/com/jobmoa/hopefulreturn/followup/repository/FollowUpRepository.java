package com.jobmoa.hopefulreturn.followup.repository;

import com.jobmoa.hopefulreturn.followup.entity.FollowUpEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowUpRepository extends JpaRepository<FollowUpEntity, Long> {

    List<FollowUpEntity> findByCourseParticipantIdOrderByFollowupIdDesc(Long courseParticipantId);

    /** 사후관리 집계 목록용 — 여러 수강건의 follow_up 스냅샷 배치 조회. */
    List<FollowUpEntity> findByCourseParticipantIdIn(Collection<Long> courseParticipantIds);
}
