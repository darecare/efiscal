package com.efiscal.backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
public class AppUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(name = "subscription_status", nullable = false, length = 30)
    private String subscriptionStatus = "ACTIVE";

    @Column(name = "subscription_start_at")
    private OffsetDateTime subscriptionStartAt;

    @Column(name = "subscription_expires_at")
    private OffsetDateTime subscriptionExpiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public ClientEntity getClient() { return client; }
    public void setClient(ClientEntity client) { this.client = client; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public RoleEntity getRole() { return role; }
    public void setRole(RoleEntity role) { this.role = role; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public OffsetDateTime getSubscriptionStartAt() { return subscriptionStartAt; }
    public void setSubscriptionStartAt(OffsetDateTime subscriptionStartAt) { this.subscriptionStartAt = subscriptionStartAt; }
    public OffsetDateTime getSubscriptionExpiresAt() { return subscriptionExpiresAt; }
    public void setSubscriptionExpiresAt(OffsetDateTime subscriptionExpiresAt) { this.subscriptionExpiresAt = subscriptionExpiresAt; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
