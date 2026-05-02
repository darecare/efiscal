package com.efiscal.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * JPA entity for paytype_map — maps external payment method codes (e.g. MerchantPro
 * payment_method_code) to fiscal payment type integers, per client.
 */
@Entity
@Table(name = "paytype_map")
public class PayTypeMapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paytype_map_id", nullable = false)
    private Long paytypeMapId;

    @Column(name = "client_id", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long clientId = 0L;

    /** External payment method code, e.g. cash_delivery, wire, intesa, raiffeisen_upc */
    @Column(name = "payment_method_code", nullable = false, length = 50)
    private String paymentMethodCode;

    /**
     * Fiscal payment type: 0=Other, 1=Cash, 2=Card, 3=Check, 4=Wire Transfer,
     * 5=Voucher, 6=Mobile Money
     */
    @Column(name = "payment_type", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Integer paymentType;

    @Column(name = "description", length = 100)
    private String description;

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

    public Long getPaytypeMapId() { return paytypeMapId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getPaymentMethodCode() { return paymentMethodCode; }
    public void setPaymentMethodCode(String paymentMethodCode) { this.paymentMethodCode = paymentMethodCode; }

    public Integer getPaymentType() { return paymentType; }
    public void setPaymentType(Integer paymentType) { this.paymentType = paymentType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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
