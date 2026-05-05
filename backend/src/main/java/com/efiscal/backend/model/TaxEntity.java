package com.efiscal.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tax")
public class TaxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tax_id", nullable = false, updatable = false)
    private Long taxId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxcategory_id", nullable = false)
    private TaxCategoryEntity taxCategory;

    @Column(name = "label", nullable = false, length = 20)
    private String label;

    @Column(name = "rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal rate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "efiscal_taxname", length = 22)
    private String efiscalTaxname;

    @Column(name = "efiscal_advanceprefix", length = 50)
    private String efiscalAdvanceprefix;

    @Column(name = "efiscal_advancename", length = 50)
    private String efiscalAdvancename;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Long getTaxId() { return taxId; }
    public void setTaxId(Long taxId) { this.taxId = taxId; }
    public TaxCategoryEntity getTaxCategory() { return taxCategory; }
    public void setTaxCategory(TaxCategoryEntity taxCategory) { this.taxCategory = taxCategory; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
    public String getEfiscalTaxname() { return efiscalTaxname; }
    public void setEfiscalTaxname(String efiscalTaxname) { this.efiscalTaxname = efiscalTaxname; }
    public String getEfiscalAdvanceprefix() { return efiscalAdvanceprefix; }
    public void setEfiscalAdvanceprefix(String efiscalAdvanceprefix) { this.efiscalAdvanceprefix = efiscalAdvanceprefix; }
    public String getEfiscalAdvancename() { return efiscalAdvancename; }
    public void setEfiscalAdvancename(String efiscalAdvancename) { this.efiscalAdvancename = efiscalAdvancename; }
}
