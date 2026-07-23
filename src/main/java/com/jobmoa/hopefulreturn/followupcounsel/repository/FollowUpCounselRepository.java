package com.jobmoa.hopefulreturn.followupcounsel.repository;

import com.jobmoa.hopefulreturn.followupcounsel.entity.FollowUpCounselEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowUpCounselRepository extends JpaRepository<FollowUpCounselEntity, Long> {

    List<FollowUpCounselEntity> findByCourseParticipantIdOrderByCounselNumberAsc(Long courseParticipantId);

    /** 사후관리 집계 목록용 — 여러 수강건의 상담 로그 배치 조회(건수·최근일 집계). */
    List<FollowUpCounselEntity> findByCourseParticipantIdIn(Collection<Long> courseParticipantIds);
}
