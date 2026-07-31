package com.jobmoa.hopefulreturn.courseparticipant.repository;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselorChangeHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounselorChangeHistoryRepository
        extends JpaRepository<CounselorChangeHistoryEntity, Long> {

    /** 수강건의 변경 이력을 최신순으로 조회한다. */
    List<CounselorChangeHistoryEntity> findByCourseParticipantIdOrderByHistoryIdDesc(Long courseParticipantId);
}
