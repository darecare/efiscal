package com.efiscal.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for fiscalbillline — individual line items on a fiscal bill.
 * Used for:
 *  - Manual fiscal bills (items entered by user)
 *  - Order-based fiscal bills (items stored after TA response)
 */
@Entity
@Table(name = "fiscalbillline")
public class FiscalBillLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fiscalbillline_id", nullable = false)
    private Long fiscalbilllineId;

    @Column(name = "fiscalbill_id", nullable = false)
    private Long fiscalbillId;

    @Column(name = "client_id", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long clientId = 0L;

    @Column(name = "org_id", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long orgId = 0L;

    /** Item name (up to 2048 chars per Tax Authority spec) */
    @Column(name = "name", nullable = false, length = 2048)
    private String name;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    /** Tax label applied to this line (e.g. "A", "E") */
    @Column(name = "tax_label", length = 10)
    private String taxLabel;

    /** Global Trade Item Number (barcode/EAN) — optional */
    @Column(name = "gtin", length = 14)
    private String gtin;

    /** External product ID from e-commerce platform */
    @Column(name = "product_id", length = 50)
    private String productId;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "isactive", length = 1, nullable = false)
    private String isactive = "Y";

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "createdby", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long createdby = 0L;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated;

    @Column(name = "updatedby", nullable = false, columnDefinition = "NUMERIC(10,0)")
    private Long updatedby = 0L;

    public Long getFiscalbilllineId() { return fiscalbilllineId; }

    public Long getFiscalbillId() { return fiscalbillId; }
    public void setFiscalbillId(Long fiscalbillId) { this.fiscalbillId = fiscalbillId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getTaxLabel() { return taxLabel; }
    public void setTaxLabel(String taxLabel) { this.taxLabel = taxLabel; }

    public String getGtin() { return gtin; }
    public void setGtin(String gtin) { this.gtin = gtin; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getIsactive() { return isactive; }
    public void setIsactive(String isactive) { this.isactive = isactive; }

    public LocalDateTime getCreated() { return created; }
    public void setCreated(LocalDateTime created) { this.created = created; }

    public Long getCreatedby() { return createdby; }
    public void setCreatedby(Long createdby) { this.createdby = createdby; }

    public LocalDateTime getUpdated() { return updated; }
    public void setUpdated(LocalDateTime updated) { this.updated = updated; }

    public Long getUpdatedby() { return updatedby; }
    public void setUpdatedby(Long updatedby) { this.updatedby = updatedby; }
}
