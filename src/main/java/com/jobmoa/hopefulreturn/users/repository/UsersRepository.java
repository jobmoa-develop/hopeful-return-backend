package com.jobmoa.hopefulreturn.users.repository;

import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<UsersEntity, Long> {

    Optional<UsersEntity> findByLoginId(String loginId);

    Optional<UsersEntity> findByLoginIdAndDeletedFalse(String loginId);

    Optional<UsersEntity> findByEmail(String email);

    List<UsersEntity> findByEnabled(Boolean enabled);

    List<UsersEntity> findByDeleted(Boolean deleted);

    Optional<UsersEntity> findTopByOrderByUserIdDesc();

    Optional<UsersEntity> findByUserIdAndDeletedFalse(Long userId);

    boolean existsByLoginIdAndDeletedFalse(String loginId);

    Page<UsersEntity> findByDeletedFalse(Pageable pageable);

    Page<UsersEntity> findByDeletedFalseAndNameContaining(String name, Pageable pageable);

    Page<UsersEntity> findByDeletedFalseAndEnabled(Boolean enabled, Pageable pageable);

    Page<UsersEntity> findByDeletedFalseAndNameContainingAndEnabled(String name, Boolean enabled, Pageable pageable);

    Page<UsersEntity> findByDeletedFalseAndUserIdIn(List<Long> userIds, Pageable pageable);

    Page<UsersEntity> findByDeletedFalseAndUserIdInAndNameContaining(List<Long> userIds, String name, Pageable pageable);

    Page<UsersEntity> findByDeletedFalseAndUserIdInAndEnabled(List<Long> userIds, Boolean enabled, Pageable pageable);

    Page<UsersEntity> findByDeletedFalseAndUserIdInAndNameContainingAndEnabled(
            List<Long> userIds,
            String name,
            Boolean enabled,
            Pageable pageable);
}
