package com.bicakclc.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.bicakclc.model.Invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceDAO {
    private Connection connection;
    
    public InvoiceDAO(Connection connection) {
        this.connection = connection;
    }
    
    public Invoice create(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices (invoice_number, company_name, company_id, invoice_date, " +
                    "quality, total_amount, discount_amount, labor_cost_amount, final_amount, total_quantity, notes, status, " +
                    "created_date, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, invoice.getInvoiceNumber());
            stmt.setString(2, invoice.getCompanyName());
            if (invoice.getCompanyId() != null) {
                stmt.setInt(3, invoice.getCompanyId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setTimestamp(4, Timestamp.valueOf(invoice.getInvoiceDate()));
            stmt.setString(5, invoice.getQuality());
            stmt.setBigDecimal(6, invoice.getTotalAmount());
            stmt.setBigDecimal(7, invoice.getDiscountAmount());
            stmt.setBigDecimal(8, invoice.getLaborCostAmount());
            stmt.setBigDecimal(9, invoice.getFinalAmount());
            stmt.setInt(10, invoice.getTotalQuantity());
            stmt.setString(11, invoice.getNotes());
            stmt.setString(12, invoice.getStatus());
            stmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(14, invoice.getCreatedBy());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating invoice failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    invoice.setInvoiceId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating invoice failed, no ID obtained.");
                }
            }
        }
        
        return invoice;
    }
    
    public Invoice findById(int invoiceId) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE invoice_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, invoiceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToInvoice(rs);
                }
            }
        }
        
        return null;
    }
    
    public List<Invoice> findByCompanyName(String companyName) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE company_name LIKE ? ORDER BY invoice_date DESC";
        List<Invoice> invoices = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + companyName + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs));
                }
            }
        }
        
        return invoices;
    }
    
    public List<Invoice> findAll() throws SQLException {
        String sql = "SELECT * FROM invoices ORDER BY invoice_date DESC";
        List<Invoice> invoices = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                invoices.add(mapResultSetToInvoice(rs));
            }
        }
        
        return invoices;
    }
    
    public void update(Invoice invoice) throws SQLException {
        String sql = "UPDATE invoices SET invoice_number = ?, company_name = ?, company_id = ?, " +
                    "invoice_date = ?, quality = ?, total_amount = ?, discount_amount = ?, " +
                    "labor_cost_amount = ?, final_amount = ?, total_quantity = ?, notes = ?, status = ?, modified_date = ?, modified_by = ? " +
                    "WHERE invoice_id = ?";
                    
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, invoice.getInvoiceNumber());
            stmt.setString(2, invoice.getCompanyName());
            if (invoice.getCompanyId() != null) {
                stmt.setInt(3, invoice.getCompanyId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setTimestamp(4, Timestamp.valueOf(invoice.getInvoiceDate()));
            stmt.setString(5, invoice.getQuality());
            stmt.setBigDecimal(6, invoice.getTotalAmount());
            stmt.setBigDecimal(7, invoice.getDiscountAmount());
            stmt.setBigDecimal(8, invoice.getLaborCostAmount());
            stmt.setBigDecimal(9, invoice.getFinalAmount());
            stmt.setInt(10, invoice.getTotalQuantity());
            stmt.setString(11, invoice.getNotes());
            stmt.setString(12, invoice.getStatus());
            stmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(14, invoice.getModifiedBy());
            stmt.setInt(15, invoice.getInvoiceId());
            
            stmt.executeUpdate();
        }
    }
    
    public void delete(int invoiceId) throws SQLException {
        String sql = "DELETE FROM invoices WHERE invoice_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, invoiceId);
            stmt.executeUpdate();
        }
    }
    
    public List<Invoice> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE invoice_date BETWEEN ? AND ? ORDER BY invoice_date DESC";
        List<Invoice> invoices = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs));
                }
            }
        }
        
        return invoices;
    }
    
    public int getNextDailySequence(String date) throws SQLException {
        // Get the highest sequence number for the given date
        String sql = "SELECT COUNT(*) FROM invoices WHERE invoice_number LIKE ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "FTR-" + date + "-%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count + 1;
                }
            }
        }
        
        return 1; // First invoice of the day
    }
    
    private Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(rs.getInt("invoice_id"));
        invoice.setInvoiceNumber(rs.getString("invoice_number"));
        invoice.setCompanyName(rs.getString("company_name"));
        
        int companyId = rs.getInt("company_id");
        if (!rs.wasNull()) {
            invoice.setCompanyId(companyId);
        }
        
        invoice.setInvoiceDate(rs.getTimestamp("invoice_date").toLocalDateTime());
        invoice.setQuality(rs.getString("quality"));
        invoice.setTotalAmount(rs.getBigDecimal("total_amount"));
        invoice.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        invoice.setLaborCostAmount(rs.getBigDecimal("labor_cost_amount"));
        invoice.setFinalAmount(rs.getBigDecimal("final_amount"));
        invoice.setTotalQuantity(rs.getInt("total_quantity"));
        invoice.setNotes(rs.getString("notes"));
        invoice.setStatus(rs.getString("status"));
        invoice.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
        invoice.setCreatedBy(rs.getString("created_by"));
        
        Timestamp modifiedDate = rs.getTimestamp("modified_date");
        if (modifiedDate != null) {
            invoice.setModifiedDate(modifiedDate.toLocalDateTime());
            invoice.setModifiedBy(rs.getString("modified_by"));
        }
        
        return invoice;
    }
} 