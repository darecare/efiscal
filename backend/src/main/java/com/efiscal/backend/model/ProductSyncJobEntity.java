package com.efiscal.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "product_sync_job")
public class ProductSyncJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sync_job_id", nullable = false, updatable = false)
    private Long syncJobId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "sync_type", nullable = false, length = 16)
    private String syncType;

    @Column(name = "synced", nullable = false)
    private int synced;

    @Column(name = "total", nullable = false)
    private int total;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "filter_from")
    private OffsetDateTime filterFrom;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    public Long getSyncJobId() { return syncJobId; }
    public void setSyncJobId(Long syncJobId) { this.syncJobId = syncJobId; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public int getSynced() { return synced; }
    public void setSynced(int synced) { this.synced = synced; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public OffsetDateTime getFilterFrom() { return filterFrom; }
    public void setFilterFrom(OffsetDateTime filterFrom) { this.filterFrom = filterFrom; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }
}
