package com.efiscal.backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "org")
public class OrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "org_id", nullable = false, updatable = false)
    private Long orgId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "RSD";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public ClientEntity getClient() { return client; }
    public void setClient(ClientEntity client) { this.client = client; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
