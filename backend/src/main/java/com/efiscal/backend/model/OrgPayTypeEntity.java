package com.efiscal.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "org_paytype")
@IdClass(OrgPayTypeId.class)
public class OrgPayTypeEntity {

    @Id
    @Column(name = "org_id")
    private Long orgId;

    @Id
    @Column(name = "payment_type")
    private Integer paymentType;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "org_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_org_paytype_org"))
    private OrgEntity org;

    public OrgPayTypeEntity() {}

    public OrgPayTypeEntity(Long orgId, Integer paymentType) {
        this.orgId = orgId;
        this.paymentType = paymentType;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Integer getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(Integer paymentType) {
        this.paymentType = paymentType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OrgEntity getOrg() {
        return org;
    }

    public void setOrg(OrgEntity org) {
        this.org = org;
    }
}
