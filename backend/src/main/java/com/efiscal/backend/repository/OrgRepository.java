package com.efiscal.backend.repository;

import com.efiscal.backend.model.OrgEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgRepository extends JpaRepository<OrgEntity, Long> {
    List<OrgEntity> findAllByDeletedAtIsNull();
    List<OrgEntity> findAllByClientClientIdAndDeletedAtIsNull(Long clientId);
}
