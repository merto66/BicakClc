package com.bicakclc.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.bicakclc.model.InvoiceItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceItemDAO {
    private Connection connection;
    
    public InvoiceItemDAO(Connection connection) {
        this.connection = connection;
    }
    
    public InvoiceItem create(InvoiceItem item) throws SQLException {
        String sql = "INSERT INTO invoice_items (invoice_id, product_id, parent_item_id, price, " +
                    "cm_value, quantity, total, is_sub_group, row_number, labor_cost, created_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, item.getInvoiceId());
            stmt.setInt(2, item.getProductId());
            
            if (item.getParentItemId() != null) {
                stmt.setInt(3, item.getParentItemId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            stmt.setBigDecimal(4, item.getPrice());
            
            if (item.getCmValue() != null) {
                stmt.setBigDecimal(5, item.getCmValue());
            } else {
                stmt.setNull(5, Types.DECIMAL);
            }
            
            stmt.setInt(6, item.getQuantity());
            stmt.setBigDecimal(7, item.getTotal());
            stmt.setBoolean(8, item.isSubGroup());
            stmt.setInt(9, item.getRowNumber());
            
            if (item.getLaborCost() != null) {
                stmt.setBigDecimal(10, item.getLaborCost());
            } else {
                stmt.setNull(10, Types.DECIMAL);
            }
            
            stmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating invoice item failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setItemId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating invoice item failed, no ID obtained.");
                }
            }
        }
        
        return item;
    }
    
    public InvoiceItem findById(int itemId) throws SQLException {
        String sql = "SELECT * FROM invoice_items WHERE item_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToInvoiceItem(rs);
                }
            }
        }
        
        return null;
    }
    
    public List<InvoiceItem> findByInvoiceId(int invoiceId) throws SQLException {
        String sql = "SELECT * FROM invoice_items WHERE invoice_id = ? ORDER BY row_number";
        List<InvoiceItem> items = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, invoiceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToInvoiceItem(rs));
                }
            }
        }
        
        return items;
    }
    
    public List<InvoiceItem> findSubItems(int parentItemId) throws SQLException {
        String sql = "SELECT * FROM invoice_items WHERE parent_item_id = ? ORDER BY row_number";
        List<InvoiceItem> items = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parentItemId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToInvoiceItem(rs));
                }
            }
        }
        
        return items;
    }
    
    public void update(InvoiceItem item) throws SQLException {
        String sql = "UPDATE invoice_items SET invoice_id = ?, product_id = ?, parent_item_id = ?, " +
                    "price = ?, cm_value = ?, quantity = ?, total = ?, is_sub_group = ?, row_number = ?, labor_cost = ? " +
                    "WHERE item_id = ?";
                    
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, item.getInvoiceId());
            stmt.setInt(2, item.getProductId());
            
            if (item.getParentItemId() != null) {
                stmt.setInt(3, item.getParentItemId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            stmt.setBigDecimal(4, item.getPrice());
            
            if (item.getCmValue() != null) {
                stmt.setBigDecimal(5, item.getCmValue());
            } else {
                stmt.setNull(5, Types.DECIMAL);
            }
            
            stmt.setInt(6, item.getQuantity());
            stmt.setBigDecimal(7, item.getTotal());
            stmt.setBoolean(8, item.isSubGroup());
            stmt.setInt(9, item.getRowNumber());
            
            if (item.getLaborCost() != null) {
                stmt.setBigDecimal(10, item.getLaborCost());
            } else {
                stmt.setNull(10, Types.DECIMAL);
            }
            
            stmt.setInt(11, item.getItemId());
            
            stmt.executeUpdate();
        }
    }
    
    public void delete(int itemId) throws SQLException {
        String sql = "DELETE FROM invoice_items WHERE item_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            stmt.executeUpdate();
        }
    }
    
    public void deleteByInvoiceId(int invoiceId) throws SQLException {
        String sql = "DELETE FROM invoice_items WHERE invoice_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, invoiceId);
            stmt.executeUpdate();
        }
    }
    
    private InvoiceItem mapResultSetToInvoiceItem(ResultSet rs) throws SQLException {
        InvoiceItem item = new InvoiceItem();
        item.setItemId(rs.getInt("item_id"));
        item.setInvoiceId(rs.getInt("invoice_id"));
        item.setProductId(rs.getInt("product_id"));
        
        int parentItemId = rs.getInt("parent_item_id");
        if (!rs.wasNull()) {
            item.setParentItemId(parentItemId);
        }
        
        item.setPrice(rs.getBigDecimal("price"));
        
        BigDecimal cmValue = rs.getBigDecimal("cm_value");
        if (cmValue != null) {
            item.setCmValue(cmValue);
        }
        
        item.setQuantity(rs.getInt("quantity"));
        item.setTotal(rs.getBigDecimal("total"));
        item.setSubGroup(rs.getBoolean("is_sub_group"));
        item.setRowNumber(rs.getInt("row_number"));
        
        BigDecimal laborCost = rs.getBigDecimal("labor_cost");
        if (laborCost != null) {
            item.setLaborCost(laborCost);
        }
        
        item.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
        
        return item;
    }
} 