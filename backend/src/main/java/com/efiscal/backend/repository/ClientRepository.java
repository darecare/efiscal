package com.efiscal.backend.repository;

import com.efiscal.backend.model.ClientEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<ClientEntity, Long> {
    List<ClientEntity> findAllByDeletedAtIsNull();
    Optional<ClientEntity> findByNameIgnoreCase(String name);
}
