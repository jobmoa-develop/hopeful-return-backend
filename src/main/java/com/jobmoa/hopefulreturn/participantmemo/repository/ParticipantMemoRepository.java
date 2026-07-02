package com.jobmoa.hopefulreturn.participantmemo.repository;

import com.jobmoa.hopefulreturn.participantmemo.entity.ParticipantMemoEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantMemoRepository extends JpaRepository<ParticipantMemoEntity, Long> {

    List<ParticipantMemoEntity> findByCourseParticipantId(Long courseParticipantId);

    List<ParticipantMemoEntity> findByUserId(Long userId);
}
