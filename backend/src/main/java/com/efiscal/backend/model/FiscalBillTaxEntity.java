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
 * JPA entity for fiscalbilltax — tax items from Tax Authority response.
 */
@Entity
@Table(name = "fiscalbilltax")
public class FiscalBillTaxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fiscalbilltax_id", nullable = false)
    private Long fiscalbilltaxId;

    @Column(name = "fiscalbill_id")
    private Long fiscalbillId;

    @Column(name = "client_id", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long clientId = 0L;

    @Column(name = "org_id", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long orgId = 0L;

    /** Tax label from TA response (e.g. "A", "E") */
    @Column(name = "efiscal_taxlabel", length = 1)
    private String efiscalTaxlabel;

    @Column(name = "efiscal_categoryname", length = 60)
    private String efiscalCategoryname;

    @Column(name = "efiscal_categorytype", columnDefinition = "NUMERIC(10,0)")
    private Long efiscalCategorytype;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "value", length = 40)
    private String value;

    @Column(name = "fiscalbilltax_uu", length = 36)
    private String fiscalbilltaxUu;

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

    public Long getFiscalbilltaxId() { return fiscalbilltaxId; }

    public Long getFiscalbillId() { return fiscalbillId; }
    public void setFiscalbillId(Long fiscalbillId) { this.fiscalbillId = fiscalbillId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public String getEfiscalTaxlabel() { return efiscalTaxlabel; }
    public void setEfiscalTaxlabel(String efiscalTaxlabel) { this.efiscalTaxlabel = efiscalTaxlabel; }

    public String getEfiscalCategoryname() { return efiscalCategoryname; }
    public void setEfiscalCategoryname(String efiscalCategoryname) { this.efiscalCategoryname = efiscalCategoryname; }

    public Long getEfiscalCategorytype() { return efiscalCategorytype; }
    public void setEfiscalCategorytype(Long efiscalCategorytype) { this.efiscalCategorytype = efiscalCategorytype; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getFiscalbilltaxUu() { return fiscalbilltaxUu; }
    public void setFiscalbilltaxUu(String fiscalbilltaxUu) { this.fiscalbilltaxUu = fiscalbilltaxUu; }

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
