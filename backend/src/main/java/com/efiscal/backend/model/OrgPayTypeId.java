package com.efiscal.backend.model;

import java.io.Serializable;
import java.util.Objects;

public class OrgPayTypeId implements Serializable {
    private Long orgId;
    private Integer paymentType;

    public OrgPayTypeId() {}

    public OrgPayTypeId(Long orgId, Integer paymentType) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrgPayTypeId that = (OrgPayTypeId) o;
        return Objects.equals(orgId, that.orgId) && Objects.equals(paymentType, that.paymentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgId, paymentType);
    }
}
