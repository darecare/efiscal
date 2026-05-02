package com.efiscal.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for fiscalbillpay — payment items per fiscal bill.
 * Stores both order-based and manually entered payment lines.
 */
@Entity
@Table(name = "fiscalbillpay")
public class FiscalBillPayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fiscalbillpay_id", nullable = false)
    private Long fiscalbillpayId;

    @Column(name = "fiscalbill_id", nullable = false)
    private Long fiscalbillId;

    @Column(name = "client_id", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long clientId = 0L;

    @Column(name = "org_id", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long orgId = 0L;

    /**
     * Fiscal payment type code:
     * 0=Other, 1=Cash, 2=Card, 3=Check, 4=Wire Transfer, 5=Voucher, 6=Mobile Money
     */
    @Column(name = "payment_type", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Integer paymentType;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "isactive", length = 1, nullable = false)
    private String isactive = "Y";

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "createdby", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long createdby = 0L;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated;

    @Column(name = "updatedby", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long updatedby = 0L;

    public Long getFiscalbillpayId() { return fiscalbillpayId; }

    public Long getFiscalbillId() { return fiscalbillId; }
    public void setFiscalbillId(Long fiscalbillId) { this.fiscalbillId = fiscalbillId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public Integer getPaymentType() { return paymentType; }
    public void setPaymentType(Integer paymentType) { this.paymentType = paymentType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getIsactive() { return isactive; }
    public void setIsactive(String isactive) { this.isactive = isactive; }

    public LocalDateTime getCreated() { return created; }
    public void setCreated(LocalDateTime created) { this.created = created; }

    public Long getCreatedby() { return createdby; }
    public void setCreatedby(Long createdby) { this.createdby = createdby; }

    public LocalDateTime getUpdated() { return updated; }
    public void setUpdated(LocalDateTime updated) { this.updated = updated; }

    public Long getUpdatedby() { return updatedby; }
    public void setUpdatedby(Long updatedby) { this.updatedby = updatedby; }
}
