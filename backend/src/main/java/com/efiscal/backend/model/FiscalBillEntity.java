package com.efiscal.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for the fiscalbill table.
 * Maps all columns from V1–V16 migrations.
 */
@Entity
@Table(name = "fiscalbill")
public class FiscalBillEntity {

    @Id
    @Column(name = "fiscalbill_id", nullable = false, length = 64)
    private String fiscalbillId;

    @Column(name = "client_id", columnDefinition = "NUMERIC(10,0)")
    private Long clientId;

    @Column(name = "org_id", columnDefinition = "NUMERIC(10,0)")
    private Long orgId;

    /** Application-level status: PENDING, SUCCESS, FAILED, RETRYING */
    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "attempt_count")
    private Integer attemptCount;

    @Column(name = "last_error", length = 512)
    private String lastError;

    @Column(name = "provider_reference", length = 128)
    private String providerReference;

    // --- Tax Authority response fields ---
    @Column(name = "efiscal_sdc_invoiceno", length = 30)
    private String efiscalSdcInvoiceno;

    @Column(name = "efiscal_sdcdatetime", length = 50)
    private String efiscalSdcdatetime;

    @Column(name = "efiscal_link", length = 2000)
    private String efiscalLink;

    @Column(name = "efiscal_qr", columnDefinition = "TEXT")
    private String efiscalQr;

    @Column(name = "efiscal_requestedby", length = 50)
    private String efiscalRequestedby;

    @Column(name = "efiscal_signedby", length = 22)
    private String efiscalSignedby;

    @Column(name = "efiscal_invoicecounter", length = 22)
    private String efiscalInvoicecounter;

    @Column(name = "efiscal_invoicecounterext", length = 22)
    private String efiscalInvoicecounterext;

    @Column(name = "efiscal_encryptedinternaldata", columnDefinition = "TEXT")
    private String efiscalEncryptedinternaldata;

    @Column(name = "efiscal_signature", columnDefinition = "TEXT")
    private String efiscalSignature;

    @Column(name = "efiscal_totalamount")
    private BigDecimal efiscalTotalamount;

    @Column(name = "efiscal_totalcounter")
    private BigDecimal efiscalTotalcounter;

    @Column(name = "efiscal_transactiontypecounter", columnDefinition = "NUMERIC(10,0)")
    private Long efiscalTransactiontypecounter;

    @Column(name = "efiscal_taxgrouprevision", columnDefinition = "NUMERIC(10,0)")
    private Long efiscalTaxgrouprevision;

    @Column(name = "efiscal_messages", length = 22)
    private String efiscalMessages;

    @Column(name = "efiscal_businessname", length = 100)
    private String efiscalBusinessname;

    @Column(name = "efiscal_tin", length = 22)
    private String efiscalTin;

    @Column(name = "efiscal_address", length = 50)
    private String efiscalAddress;

    @Column(name = "efiscal_mrc", length = 22)
    private String efiscalMrc;

    @Column(name = "efiscal_code", length = 1)
    private String efiscalCode;

    @Column(name = "efiscal_name", length = 50)
    private String efiscalName;

    // --- Invoice type/transaction type ---
    /** 0=Normal, 1=Proforma, 2=Copy, 3=Training, 4=Advance */
    @Column(name = "efiscal_invoicetype", columnDefinition = "NUMERIC(10,0)")
    private Integer efiscalInvoicetype;

    /** 0=Sale, 1=Refund */
    @Column(name = "efiscal_transactiontype", columnDefinition = "NUMERIC(10,0)")
    private Integer efiscalTransactiontype;

    @Column(name = "efiscal_type", length = 2)
    private String efiscalType;

    @Column(name = "efiscal_customername", length = 100)
    private String efiscalCustomername;

    // --- Order reference (external order id from e-commerce platform) ---
    @Column(name = "order_id", length = 64)
    private String orderId;

    // --- Legacy audit fields ---
    @Column(name = "value", length = 40)
    private String value;

    @Column(name = "fiscalbill_uu", length = 36)
    private String fiscalbillUu;

    @Column(name = "isactive", length = 1, nullable = false)
    private String isactive = "Y";

    @Column(name = "processed", length = 1, nullable = false)
    private String processed = "N";

    @Column(name = "processedon")
    private BigDecimal processedon;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "createdby", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long createdby = 0L;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated;

    @Column(name = "updatedby", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long updatedby = 0L;

    // Getters and setters

    public String getFiscalbillId() { return fiscalbillId; }
    public void setFiscalbillId(String fiscalbillId) { this.fiscalbillId = fiscalbillId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }

    public String getEfiscalSdcInvoiceno() { return efiscalSdcInvoiceno; }
    public void setEfiscalSdcInvoiceno(String v) { this.efiscalSdcInvoiceno = v; }

    public String getEfiscalSdcdatetime() { return efiscalSdcdatetime; }
    public void setEfiscalSdcdatetime(String v) { this.efiscalSdcdatetime = v; }

    public String getEfiscalLink() { return efiscalLink; }
    public void setEfiscalLink(String v) { this.efiscalLink = v; }

    public String getEfiscalQr() { return efiscalQr; }
    public void setEfiscalQr(String v) { this.efiscalQr = v; }

    public String getEfiscalRequestedby() { return efiscalRequestedby; }
    public void setEfiscalRequestedby(String v) { this.efiscalRequestedby = v; }

    public String getEfiscalSignedby() { return efiscalSignedby; }
    public void setEfiscalSignedby(String v) { this.efiscalSignedby = v; }

    public String getEfiscalInvoicecounter() { return efiscalInvoicecounter; }
    public void setEfiscalInvoicecounter(String v) { this.efiscalInvoicecounter = v; }

    public String getEfiscalInvoicecounterext() { return efiscalInvoicecounterext; }
    public void setEfiscalInvoicecounterext(String v) { this.efiscalInvoicecounterext = v; }

    public String getEfiscalEncryptedinternaldata() { return efiscalEncryptedinternaldata; }
    public void setEfiscalEncryptedinternaldata(String v) { this.efiscalEncryptedinternaldata = v; }

    public String getEfiscalSignature() { return efiscalSignature; }
    public void setEfiscalSignature(String v) { this.efiscalSignature = v; }

    public BigDecimal getEfiscalTotalamount() { return efiscalTotalamount; }
    public void setEfiscalTotalamount(BigDecimal v) { this.efiscalTotalamount = v; }

    public BigDecimal getEfiscalTotalcounter() { return efiscalTotalcounter; }
    public void setEfiscalTotalcounter(BigDecimal v) { this.efiscalTotalcounter = v; }

    public Long getEfiscalTransactiontypecounter() { return efiscalTransactiontypecounter; }
    public void setEfiscalTransactiontypecounter(Long v) { this.efiscalTransactiontypecounter = v; }

    public Long getEfiscalTaxgrouprevision() { return efiscalTaxgrouprevision; }
    public void setEfiscalTaxgrouprevision(Long v) { this.efiscalTaxgrouprevision = v; }

    public String getEfiscalMessages() { return efiscalMessages; }
    public void setEfiscalMessages(String v) { this.efiscalMessages = v; }

    public String getEfiscalBusinessname() { return efiscalBusinessname; }
    public void setEfiscalBusinessname(String v) { this.efiscalBusinessname = v; }

    public String getEfiscalTin() { return efiscalTin; }
    public void setEfiscalTin(String v) { this.efiscalTin = v; }

    public String getEfiscalAddress() { return efiscalAddress; }
    public void setEfiscalAddress(String v) { this.efiscalAddress = v; }

    public String getEfiscalMrc() { return efiscalMrc; }
    public void setEfiscalMrc(String v) { this.efiscalMrc = v; }

    public String getEfiscalCode() { return efiscalCode; }
    public void setEfiscalCode(String v) { this.efiscalCode = v; }

    public String getEfiscalName() { return efiscalName; }
    public void setEfiscalName(String v) { this.efiscalName = v; }

    public Integer getEfiscalInvoicetype() { return efiscalInvoicetype; }
    public void setEfiscalInvoicetype(Integer v) { this.efiscalInvoicetype = v; }

    public Integer getEfiscalTransactiontype() { return efiscalTransactiontype; }
    public void setEfiscalTransactiontype(Integer v) { this.efiscalTransactiontype = v; }

    public String getEfiscalType() { return efiscalType; }
    public void setEfiscalType(String v) { this.efiscalType = v; }

    public String getEfiscalCustomername() { return efiscalCustomername; }
    public void setEfiscalCustomername(String v) { this.efiscalCustomername = v; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getFiscalbillUu() { return fiscalbillUu; }
    public void setFiscalbillUu(String fiscalbillUu) { this.fiscalbillUu = fiscalbillUu; }

    public String getIsactive() { return isactive; }
    public void setIsactive(String isactive) { this.isactive = isactive; }

    public String getProcessed() { return processed; }
    public void setProcessed(String processed) { this.processed = processed; }

    public BigDecimal getProcessedon() { return processedon; }
    public void setProcessedon(BigDecimal processedon) { this.processedon = processedon; }

    public LocalDateTime getCreated() { return created; }
    public void setCreated(LocalDateTime created) { this.created = created; }

    public Long getCreatedby() { return createdby; }
    public void setCreatedby(Long createdby) { this.createdby = createdby; }

    public LocalDateTime getUpdated() { return updated; }
    public void setUpdated(LocalDateTime updated) { this.updated = updated; }

    public Long getUpdatedby() { return updatedby; }
    public void setUpdatedby(Long updatedby) { this.updatedby = updatedby; }
}
