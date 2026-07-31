package com.ai.server.repository;

import com.ai.server.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByActivateToken(String activateToken);

    Optional<UserEntity> findByResetToken(String resetToken);

    Boolean existsByEmail(String email);
}
