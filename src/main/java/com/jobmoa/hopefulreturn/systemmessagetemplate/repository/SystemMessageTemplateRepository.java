package com.jobmoa.hopefulreturn.systemmessagetemplate.repository;

import com.jobmoa.hopefulreturn.systemmessagetemplate.entity.SystemMessageTemplateEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemMessageTemplateRepository extends JpaRepository<SystemMessageTemplateEntity, Long> {

    Optional<SystemMessageTemplateEntity> findByTemplateKey(String templateKey);

    List<SystemMessageTemplateEntity> findAllByOrderByTemplateKeyAsc();
}
