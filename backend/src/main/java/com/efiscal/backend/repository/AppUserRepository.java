package com.efiscal.backend.repository;

import com.efiscal.backend.model.AppUserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {
    List<AppUserEntity> findAllByDeletedAtIsNull();
    Optional<AppUserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
