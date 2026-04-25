package com.efiscal.backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "apiconn")
public class ApiConnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "apiconn_id", nullable = false, updatable = false)
    private Long apiconnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private OrgEntity org;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "api_platform", nullable = false, length = 50)
    private String apiPlatform;

    @Column(name = "api_base_url", length = 500)
    private String apiBaseUrl;

    @Column(name = "apiauthtype", length = 50)
    private String apiauthtype;

    @Column(name = "apikey", length = 255)
    private String apikey;

    @Column(name = "apisecret", length = 255)
    private String apisecret;

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

    public Long getApiconnId() { return apiconnId; }
    public OrgEntity getOrg() { return org; }
    public void setOrg(OrgEntity org) { this.org = org; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getApiPlatform() { return apiPlatform; }
    public void setApiPlatform(String apiPlatform) { this.apiPlatform = apiPlatform; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getApiauthtype() { return apiauthtype; }
    public void setApiauthtype(String apiauthtype) { this.apiauthtype = apiauthtype; }
    public String getApikey() { return apikey; }
    public void setApikey(String apikey) { this.apikey = apikey; }
    public String getApisecret() { return apisecret; }
    public void setApisecret(String apisecret) { this.apisecret = apisecret; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
