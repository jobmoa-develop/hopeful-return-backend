package com.jobmoa.hopefulreturn.participantsms.repository;

import com.jobmoa.hopefulreturn.participantsms.entity.ParticipantSmsImageEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantSmsImageRepository extends JpaRepository<ParticipantSmsImageEntity, Long> {

    List<ParticipantSmsImageEntity> findBySmsIdOrderBySortOrderAsc(Long smsId);

    List<ParticipantSmsImageEntity> findBySmsIdIn(Collection<Long> smsIds);
}
