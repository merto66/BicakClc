package com.bicakclc.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Invoice {
    private Integer invoiceId;
    private String invoiceNumber;
    private String companyName;
    private Integer companyId;
    private LocalDateTime invoiceDate;
    private String quality;
    private BigDecimal totalAmount;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal laborCostAmount;
    private BigDecimal finalAmount;
    private Integer totalQuantity;
    private String notes;
    private String status;
    private List<InvoiceItem> items;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime modifiedDate;
    private String modifiedBy;

    public Invoice() {
        this.items = new ArrayList<>();
        this.invoiceDate = LocalDateTime.now();
        this.status = "DRAFT";
        this.totalAmount = BigDecimal.ZERO;
        this.discountPercentage = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.laborCostAmount = BigDecimal.ZERO;
        this.finalAmount = BigDecimal.ZERO;
        this.totalQuantity = 0;
    }

    public Invoice(String companyName, String quality) {
        this();
        this.companyName = companyName;
        this.quality = quality;
    }

    // Getters and Setters
    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public LocalDateTime getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDateTime invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        calculateFinalAmount();
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
        if (this.totalAmount != null && discountPercentage != null) {
            this.discountAmount = totalAmount.multiply(discountPercentage)
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            calculateFinalAmount();
        }
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        calculateFinalAmount();
    }

    public BigDecimal getLaborCostAmount() {
        return laborCostAmount;
    }

    public void setLaborCostAmount(BigDecimal laborCostAmount) {
        this.laborCostAmount = laborCostAmount;
        calculateFinalAmount();
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items;
        calculateTotalAmount();
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    // Helper methods
    public void addItem(InvoiceItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        calculateTotalAmount();
    }

    public void removeItem(InvoiceItem item) {
        if (items != null) {
            items.remove(item);
            calculateTotalAmount();
        }
    }

    private void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(InvoiceItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        calculateFinalAmount();
    }

    private void calculateFinalAmount() {
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
        if (laborCostAmount == null) {
            laborCostAmount = BigDecimal.ZERO;
        }
        this.finalAmount = totalAmount.subtract(discountAmount).add(laborCostAmount);
    }

    @Override
    public String toString() {
        return String.format("Invoice{number='%s', company='%s', total=%s, final=%s}",
                invoiceNumber, companyName, totalAmount, finalAmount);
    }
} 