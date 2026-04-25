package com.efiscal.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserOrgAccessId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    public UserOrgAccessId() {}

    public UserOrgAccessId(Long userId, Long orgId) {
        this.userId = userId;
        this.orgId = orgId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserOrgAccessId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(orgId, that.orgId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, orgId);
    }
}
