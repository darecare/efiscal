package com.efiscal.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "fiscal_bill_idempotency_keys")
public class FiscalBillIdempotencyKeyEntity {

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_document_id", nullable = false)
    private FiscalBillEntity fiscalBill;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public FiscalBillEntity getFiscalBill() {
        return fiscalBill;
    }

    public void setFiscalBill(FiscalBillEntity fiscalBill) {
        this.fiscalBill = fiscalBill;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
