package com.jobmoa.hopefulreturn.staffunavailablenotice.repository;

import com.jobmoa.hopefulreturn.staffunavailablenotice.entity.StaffUnavailableNoticeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffUnavailableNoticeRepository
        extends JpaRepository<StaffUnavailableNoticeEntity, Long> {

    /** 특정 staff_schedule 행에 대해 해당 수신자에게 이미 발송 이력이 있는지(감사·조회용). */
    boolean existsByStaffScheduleIdAndRecipientUserId(Long staffScheduleId, Long recipientUserId);
}
