package com.jobmoa.hopefulreturn.userrole.repository;

import com.jobmoa.hopefulreturn.userrole.entity.UserRoleEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    @EntityGraph(attributePaths = {"user", "role"})
    List<UserRoleEntity> findAll();

    @EntityGraph(attributePaths = {"user", "role"})
    List<UserRoleEntity> findByUserId(Long userId);

    List<UserRoleEntity> findByRoleId(Long roleId);

    List<UserRoleEntity> findByUserIdAndRoleId(Long userId, Long roleId);

    Optional<UserRoleEntity> findTopByOrderByUserRoleIdDesc();

    void deleteByUserId(Long userId);
}