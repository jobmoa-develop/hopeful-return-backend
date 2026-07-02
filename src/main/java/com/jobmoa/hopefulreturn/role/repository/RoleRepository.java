package com.jobmoa.hopefulreturn.role.repository;

import com.jobmoa.hopefulreturn.role.entity.RoleEntity;
import com.jobmoa.hopefulreturn.role.entity.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByRoleName(RoleName roleName);
}
