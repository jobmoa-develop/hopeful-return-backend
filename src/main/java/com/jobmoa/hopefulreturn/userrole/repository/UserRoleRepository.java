package com.jobmoa.hopefulreturn.userrole.repository;

import com.jobmoa.hopefulreturn.userrole.entity.UserRoleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    List<UserRoleEntity> findByUserId(Long userId);

    List<UserRoleEntity> findByRoleId(Long roleId);

    List<UserRoleEntity> findByUserIdAndRoleId(Long userId, Long roleId);
}
