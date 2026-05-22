package com.efiscal.backend.model;

import java.io.Serializable;
import java.util.Objects;

public class RoleActionAccessId implements Serializable {
    private Long roleId;
    private Long actionId;

    public RoleActionAccessId() {}

    public RoleActionAccessId(Long roleId, Long actionId) {
        this.roleId = roleId;
        this.actionId = actionId;
    }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleActionAccessId that = (RoleActionAccessId) o;
        return Objects.equals(roleId, that.roleId) &&
               Objects.equals(actionId, that.actionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, actionId);
    }
}
