package com.jobmoa.hopefulreturn.participant.repository;

import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<ParticipantEntity, Long> {

    List<ParticipantEntity> findByPhone(String phone);

    Optional<ParticipantEntity> findByMatchKey(String matchKey);

    List<ParticipantEntity> findByName(String name);

    boolean existsByPhone(String phone);

    Optional<ParticipantEntity> findFirstByPhoneOrderByParticipantIdAsc(String phone);

    Page<ParticipantEntity> findByNameContaining(String name, Pageable pageable);

    Page<ParticipantEntity> findByPhoneContaining(String phone, Pageable pageable);

    Page<ParticipantEntity> findByNameContainingAndPhoneContaining(String name, String phone, Pageable pageable);
}
