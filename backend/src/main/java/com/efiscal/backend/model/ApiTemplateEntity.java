package com.efiscal.backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "apitemplate")
public class ApiTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "apitemplate_id", nullable = false, updatable = false)
    private Long apitemplateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apiconn_id", nullable = false)
    private ApiConnEntity apiConn;

    @Column(name = "operation_key", nullable = false, length = 120)
    private String operationKey;

    @Column(name = "http_method", nullable = false, length = 16)
    private String httpMethod;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType = "application/json";

    @Column(name = "endpoint_path", nullable = false, length = 500)
    private String endpointPath;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getApitemplateId() { return apitemplateId; }
    public ApiConnEntity getApiConn() { return apiConn; }
    public void setApiConn(ApiConnEntity apiConn) { this.apiConn = apiConn; }
    public String getOperationKey() { return operationKey; }
    public void setOperationKey(String operationKey) { this.operationKey = operationKey; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getEndpointPath() { return endpointPath; }
    public void setEndpointPath(String endpointPath) { this.endpointPath = endpointPath; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
