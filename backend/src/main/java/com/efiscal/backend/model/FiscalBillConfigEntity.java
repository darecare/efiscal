package com.efiscal.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * JPA entity for fiscalbillconfig — org-level fiscal configuration.
 * Stores esirno (POS invoice number prefix for Tax Authority invoiceNumber field)
 * and email settings for post-fiscalization notifications.
 */
@Entity
@Table(name = "fiscalbillconfig")
public class FiscalBillConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fiscalbillconfig_id", nullable = false)
    private Long fiscalbillconfigId;

    @Column(name = "client_id", columnDefinition = "NUMERIC(10,0)")
    private Long clientId = 0L;

    @Column(name = "org_id", columnDefinition = "NUMERIC(10,0)")
    private Long orgId = 0L;

    /** POS invoice number / ESIR number used as invoiceNumber in Tax Authority request */
    @Column(name = "esirno", length = 22)
    private String esirno;

    @Column(name = "istest", length = 1, nullable = false)
    private String istest = "N";

    @Column(name = "email_from", length = 60)
    private String emailFrom;

    @Column(name = "email_bcc", length = 60)
    private String emailBcc;

    @Column(name = "email_test", length = 60)
    private String emailTest;

    @Column(name = "fiscalbillconfig_uu", length = 36)
    private String fiscalbillconfigUu;

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

    public Long getFiscalbillconfigId() { return fiscalbillconfigId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public String getEsirno() { return esirno; }
    public void setEsirno(String esirno) { this.esirno = esirno; }

    public String getIstest() { return istest; }
    public void setIstest(String istest) { this.istest = istest; }

    public String getEmailFrom() { return emailFrom; }
    public void setEmailFrom(String emailFrom) { this.emailFrom = emailFrom; }

    public String getEmailBcc() { return emailBcc; }
    public void setEmailBcc(String emailBcc) { this.emailBcc = emailBcc; }

    public String getEmailTest() { return emailTest; }
    public void setEmailTest(String emailTest) { this.emailTest = emailTest; }

    public String getFiscalbillconfigUu() { return fiscalbillconfigUu; }
    public void setFiscalbillconfigUu(String fiscalbillconfigUu) { this.fiscalbillconfigUu = fiscalbillconfigUu; }

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
