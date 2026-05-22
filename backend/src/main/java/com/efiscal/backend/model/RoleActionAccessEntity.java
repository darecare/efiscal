package com.efiscal.backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "role_action_access")
@IdClass(RoleActionAccessId.class)
public class RoleActionAccessEntity {

    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Id
    @Column(name = "action_id", nullable = false)
    private Long actionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", insertable = false, updatable = false)
    private ActionCatalogEntity action;

    @Column(name = "is_allowed", nullable = false)
    private boolean isAllowed = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
    public RoleEntity getRole() { return role; }
    public void setRole(RoleEntity role) { this.role = role; }
    public ActionCatalogEntity getAction() { return action; }
    public void setAction(ActionCatalogEntity action) { this.action = action; }
    public boolean isAllowed() { return isAllowed; }
    public void setAllowed(boolean allowed) { isAllowed = allowed; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
