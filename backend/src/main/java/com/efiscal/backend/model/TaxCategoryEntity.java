package com.efiscal.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "taxcategory")
public class TaxCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "taxcategory_id", nullable = false, updatable = false)
    private Long taxCategoryId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "taxcategory_code", length = 10)
    private String taxcategoryCode;

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

    public Long getTaxCategoryId() { return taxCategoryId; }
    public void setTaxCategoryId(Long taxCategoryId) { this.taxCategoryId = taxCategoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTaxcategoryCode() { return taxcategoryCode; }
    public void setTaxcategoryCode(String taxcategoryCode) { this.taxcategoryCode = taxcategoryCode; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
