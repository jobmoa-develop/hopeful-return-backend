package com.jobmoa.hopefulreturn.users.repository;

import com.jobmoa.hopefulreturn.users.entity.UsersEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<UsersEntity, Long> {

    Optional<UsersEntity> findByLoginId(String loginId);

    Optional<UsersEntity> findByEmail(String email);

    List<UsersEntity> findByEnabled(Boolean enabled);

    List<UsersEntity> findByDeleted(Boolean deleted);
}
