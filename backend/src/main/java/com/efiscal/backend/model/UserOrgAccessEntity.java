package com.efiscal.backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_orgaccess")
public class UserOrgAccessEntity {

    @EmbeddedId
    private UserOrgAccessId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private AppUserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("orgId")
    @JoinColumn(name = "org_id")
    private OrgEntity org;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UserOrgAccessId getId() { return id; }
    public void setId(UserOrgAccessId id) { this.id = id; }
    public AppUserEntity getUser() { return user; }
    public void setUser(AppUserEntity user) { this.user = user; }
    public OrgEntity getOrg() { return org; }
    public void setOrg(OrgEntity org) { this.org = org; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
