package com.bicakclc.service;

import com.bicakclc.dao.InvoiceDAO;
import com.bicakclc.model.Invoice;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class InvoiceService {
    private final InvoiceDAO invoiceDAO;
    private final Connection connection;

    public InvoiceService(Connection connection) {
        this.connection = connection;
        this.invoiceDAO = new InvoiceDAO(connection);
    }

    public Invoice createInvoice(Invoice invoice) throws SQLException {
        try {
            connection.setAutoCommit(false);
            
            // Fatura numarası kontrolü
            if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Fatura numarası boş olamaz.");
            }

            // Firma adı kontrolü
            if (invoice.getCompanyName() == null || invoice.getCompanyName().trim().isEmpty()) {
                throw new IllegalArgumentException("Firma adı boş olamaz.");
            }

            // Fatura tarihi kontrolü
            if (invoice.getInvoiceDate() == null) {
                invoice.setInvoiceDate(LocalDateTime.now());
            }

            // Status kontrolü
            if (invoice.getStatus() == null || invoice.getStatus().trim().isEmpty()) {
                invoice.setStatus("DRAFT"); // Varsayılan durum
            }

            // Tutar hesaplamaları kontrolü
            if (invoice.getTotalAmount() == null) {
                throw new IllegalArgumentException("Toplam tutar boş olamaz.");
            }

            Invoice createdInvoice = invoiceDAO.create(invoice);
            connection.commit();
            return createdInvoice;
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public Invoice updateInvoice(Invoice invoice) throws SQLException {
        try {
            connection.setAutoCommit(false);

            // ID kontrolü
            if (invoice.getInvoiceId() <= 0) {
                throw new IllegalArgumentException("Fatura ID'si boş olamaz.");
            }

            // Mevcut faturayı kontrol et
            Invoice existingInvoice = invoiceDAO.findById(invoice.getInvoiceId());
            if (existingInvoice == null) {
                throw new IllegalArgumentException("Güncellenecek fatura bulunamadı.");
            }

            invoiceDAO.update(invoice);
            connection.commit();
            return invoice;
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public void deleteInvoice(int invoiceId) throws SQLException {
        try {
            connection.setAutoCommit(false);

            Invoice invoice = invoiceDAO.findById(invoiceId);
            if (invoice == null) {
                throw new IllegalArgumentException("Silinecek fatura bulunamadı.");
            }

            invoiceDAO.delete(invoiceId);
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public Invoice getInvoiceById(int invoiceId) throws SQLException {
        Invoice invoice = invoiceDAO.findById(invoiceId);
        if (invoice == null) {
            throw new IllegalArgumentException("Fatura bulunamadı.");
        }
        return invoice;
    }

    public List<Invoice> getAllInvoices() throws SQLException {
        return invoiceDAO.findAll();
    }

    public List<Invoice> searchInvoicesByCompany(String companyName) throws SQLException {
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Firma adı boş olamaz.");
        }
        return invoiceDAO.findByCompanyName(companyName);
    }

    public List<Invoice> getInvoicesByDateRange(LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Başlangıç ve bitiş tarihleri boş olamaz.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Başlangıç tarihi bitiş tarihinden sonra olamaz.");
        }
        return invoiceDAO.findByDateRange(startDate, endDate);
    }

    public void finalizeInvoice(int invoiceId) throws SQLException {
        try {
            connection.setAutoCommit(false);

            Invoice invoice = invoiceDAO.findById(invoiceId);
            if (invoice == null) {
                throw new IllegalArgumentException("Fatura bulunamadı.");
            }

            // Status kontrolü
            if ("COMPLETED".equals(invoice.getStatus())) {
                throw new IllegalStateException("Fatura zaten tamamlanmış durumda.");
            }

            // Fatura kalemlerinin kontrolü yapılabilir
            // Toplam tutarların doğruluğu kontrol edilebilir

            invoice.setStatus("COMPLETED");
            invoice.setModifiedDate(LocalDateTime.now());
            invoiceDAO.update(invoice);

            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public void validateInvoice(Invoice invoice) {
        List<String> errors = new ArrayList<>();

        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().trim().isEmpty()) {
            errors.add("Fatura numarası boş olamaz.");
        }

        if (invoice.getCompanyName() == null || invoice.getCompanyName().trim().isEmpty()) {
            errors.add("Firma adı boş olamaz.");
        }

        if (invoice.getInvoiceDate() == null) {
            errors.add("Fatura tarihi boş olamaz.");
        }

        if (invoice.getTotalAmount() == null) {
            errors.add("Toplam tutar boş olamaz.");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
    }
    
    public int getNextDailySequence(String date) throws SQLException {
        return invoiceDAO.getNextDailySequence(date);
    }
} 