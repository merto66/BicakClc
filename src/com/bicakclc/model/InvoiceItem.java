package com.bicakclc.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceItem {
    private Integer itemId;
    private Integer invoiceId;
    private Integer productId;
    private Integer parentItemId;
    private BigDecimal price;
    private BigDecimal cmValue;
    private Integer quantity;
    private BigDecimal total;
    private boolean subGroup;
    private Integer rowNumber;
    private BigDecimal laborCost;
    private LocalDateTime createdDate;
    
    // Product information (not in database, for display purposes)
    private String productName;
    private String productCode;
    private String unit;
    
    public InvoiceItem() {
        this.quantity = 1;
        this.subGroup = false;
    }
    
    // Getters and Setters
    public Integer getItemId() {
        return itemId;
    }
    
    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }
    
    public Integer getInvoiceId() {
        return invoiceId;
    }
    
    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }
    
    public Integer getProductId() {
        return productId;
    }
    
    public void setProductId(Integer productId) {
        this.productId = productId;
    }
    
    public Integer getParentItemId() {
        return parentItemId;
    }
    
    public void setParentItemId(Integer parentItemId) {
        this.parentItemId = parentItemId;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public BigDecimal getCmValue() {
        return cmValue;
    }
    
    public void setCmValue(BigDecimal cmValue) {
        this.cmValue = cmValue;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getTotal() {
        return total;
    }
    
    public void setTotal(BigDecimal total) {
        this.total = total;
    }
    
    public boolean isSubGroup() {
        return subGroup;
    }
    
    public void setSubGroup(boolean subGroup) {
        this.subGroup = subGroup;
    }
    
    public Integer getRowNumber() {
        return rowNumber;
    }
    
    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }
    
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductCode() {
        return productCode;
    }
    
    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public BigDecimal getLaborCost() {
        return laborCost;
    }
    
    public void setLaborCost(BigDecimal laborCost) {
        this.laborCost = laborCost;
    }
    
    // Helper methods
    public boolean hasParent() {
        return parentItemId != null;
    }
    
    public boolean hasCmValue() {
        return cmValue != null;
    }
    
    @Override
    public String toString() {
        return String.format("InvoiceItem{id=%d, productName='%s', quantity=%d, total=%s}",
                itemId, productName, quantity, total);
    }
} 