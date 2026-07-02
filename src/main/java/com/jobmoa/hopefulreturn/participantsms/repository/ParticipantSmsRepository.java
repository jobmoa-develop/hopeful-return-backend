package com.jobmoa.hopefulreturn.participantsms.repository;

import com.jobmoa.hopefulreturn.participantsms.entity.ParticipantSmsEntity;
import com.jobmoa.hopefulreturn.participantsms.entity.SendStatus;
import com.jobmoa.hopefulreturn.participantsms.entity.SmsType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantSmsRepository extends JpaRepository<ParticipantSmsEntity, Long> {

    List<ParticipantSmsEntity> findByCourseParticipantId(Long courseParticipantId);

    List<ParticipantSmsEntity> findBySentBy(Long sentBy);

    List<ParticipantSmsEntity> findBySmsType(SmsType smsType);

    List<ParticipantSmsEntity> findBySendStatus(SendStatus sendStatus);
}
