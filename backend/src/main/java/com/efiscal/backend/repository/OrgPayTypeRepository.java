package com.efiscal.backend.repository;

import com.efiscal.backend.model.OrgPayTypeEntity;
import com.efiscal.backend.model.OrgPayTypeId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgPayTypeRepository extends JpaRepository<OrgPayTypeEntity, OrgPayTypeId> {
    List<OrgPayTypeEntity> findByOrgId(Long orgId);
    void deleteByOrgId(Long orgId);
}
