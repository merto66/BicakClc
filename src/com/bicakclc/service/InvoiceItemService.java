package com.bicakclc.service;

import com.bicakclc.dao.InvoiceItemDAO;
import com.bicakclc.model.InvoiceItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.math.BigDecimal;

public class InvoiceItemService {
    private final InvoiceItemDAO invoiceItemDAO;
    private final Connection connection;

    public InvoiceItemService(Connection connection) {
        this.connection = connection;
        this.invoiceItemDAO = new InvoiceItemDAO(connection);
    }

    public InvoiceItem createInvoiceItem(InvoiceItem item) throws SQLException {
        try {
            connection.setAutoCommit(false);
            
            validateInvoiceItem(item);
            calculateTotal(item);
            
            InvoiceItem createdItem = invoiceItemDAO.create(item);
            connection.commit();
            return createdItem;
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public List<InvoiceItem> createBulkInvoiceItems(List<InvoiceItem> items) throws SQLException {
        try {
            connection.setAutoCommit(false);
            
            for (InvoiceItem item : items) {
                validateInvoiceItem(item);
                calculateTotal(item);
                invoiceItemDAO.create(item);
            }
            
            connection.commit();
            return items;
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public InvoiceItem updateInvoiceItem(InvoiceItem item) throws SQLException {
        try {
            connection.setAutoCommit(false);
            
            if (item.getItemId() <= 0) {
                throw new IllegalArgumentException("Kalem ID'si boş olamaz.");
            }

            InvoiceItem existingItem = invoiceItemDAO.findById(item.getItemId());
            if (existingItem == null) {
                throw new IllegalArgumentException("Güncellenecek kalem bulunamadı.");
            }

            validateInvoiceItem(item);
            calculateTotal(item);
            
            invoiceItemDAO.update(item);
            connection.commit();
            return item;
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public void deleteInvoiceItem(int itemId) throws SQLException {
        try {
            connection.setAutoCommit(false);

            InvoiceItem item = invoiceItemDAO.findById(itemId);
            if (item == null) {
                throw new IllegalArgumentException("Silinecek kalem bulunamadı.");
            }

            // Alt kalemleri de sil
            List<InvoiceItem> subItems = invoiceItemDAO.findSubItems(itemId);
            for (InvoiceItem subItem : subItems) {
                invoiceItemDAO.delete(subItem.getItemId());
            }

            invoiceItemDAO.delete(itemId);
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public List<InvoiceItem> getInvoiceItems(int invoiceId) throws SQLException {
        return invoiceItemDAO.findByInvoiceId(invoiceId);
    }
    
    public List<InvoiceItem> getInvoiceItemsByInvoiceId(int invoiceId) throws SQLException {
        return invoiceItemDAO.findByInvoiceId(invoiceId);
    }

    public List<InvoiceItem> getSubItems(int parentItemId) throws SQLException {
        return invoiceItemDAO.findSubItems(parentItemId);
    }

    private void validateInvoiceItem(InvoiceItem item) {
        if (item.getInvoiceId() <= 0) {
            throw new IllegalArgumentException("Fatura ID'si geçersiz.");
        }

        if (item.getProductId() <= 0) {
            throw new IllegalArgumentException("Ürün ID'si geçersiz.");
        }

        if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fiyat geçersiz.");
        }

        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Miktar geçersiz.");
        }

        // Alt kalem kontrolü - parentItemId zorunlu değil, sadece isSubGroup yeterli
        // if (item.isSubGroup() && item.getParentItemId() == null) {
        //     throw new IllegalArgumentException("Alt kalem için üst kalem ID'si gerekli.");
        // }
    }

    private void calculateTotal(InvoiceItem item) {
        BigDecimal total;
        
        if (item.getCmValue() != null) {
            // CM değeri varsa: fiyat * miktar * cm_value
            total = item.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .multiply(item.getCmValue());
        } else {
            // CM değeri yoksa: fiyat * miktar
            total = item.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
        }
        
        item.setTotal(total);
    }
} 