package com.jobmoa.hopefulreturn.adminsms.repository;

import com.jobmoa.hopefulreturn.adminsms.entity.AdminSmsImageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSmsImageRepository extends JpaRepository<AdminSmsImageEntity, Long> {

    List<AdminSmsImageEntity> findByAdminSmsIdOrderBySortOrderAsc(Long adminSmsId);

    List<AdminSmsImageEntity> findByAdminSmsIdIn(List<Long> adminSmsIds);
}
