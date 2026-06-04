package com.efiscal.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "product")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "mp_product_id")
    private Long mpProductId;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Column(name = "sku", length = 255)
    private String sku;

    @Column(name = "ean", length = 100)
    private String ean;

    @Column(name = "last_known_price", precision = 14, scale = 2)
    private BigDecimal lastKnownPrice;

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

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getMpProductId() { return mpProductId; }
    public void setMpProductId(Long mpProductId) { this.mpProductId = mpProductId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getEan() { return ean; }
    public void setEan(String ean) { this.ean = ean; }
    public BigDecimal getLastKnownPrice() { return lastKnownPrice; }
    public void setLastKnownPrice(BigDecimal lastKnownPrice) { this.lastKnownPrice = lastKnownPrice; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
