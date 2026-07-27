package com.jobmoa.hopefulreturn.smstemplate.repository;

import com.jobmoa.hopefulreturn.smstemplate.entity.SmsTemplateEntity;
import com.jobmoa.hopefulreturn.smstemplate.entity.SmsTemplateScope;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmsTemplateRepository extends JpaRepository<SmsTemplateEntity, Long> {

    /**
     * 특정 계정에게 보이는 템플릿: 공용(SHARED) 전체 + 본인 소유 개인(PERSONAL).
     */
    @Query("select t from SmsTemplateEntity t "
            + "where t.scope = :sharedScope or t.userId = :userId "
            + "order by t.updatedAt desc, t.smsTemplateId desc")
    List<SmsTemplateEntity> findVisibleTo(
            @Param("sharedScope") SmsTemplateScope sharedScope,
            @Param("userId") Long userId);
}
