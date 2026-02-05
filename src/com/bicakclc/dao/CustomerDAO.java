package com.bicakclc.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bicakclc.model.Customer;

public class CustomerDAO {
    private Connection connection;
    
    public CustomerDAO(Connection connection) {
        this.connection = connection;
    }
    
    public void createCustomer(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (company_name, contact_person, phone, email, address, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, customer.getCompanyName());
            stmt.setString(2, customer.getContactPerson());
            stmt.setString(3, customer.getPhone());
            stmt.setString(4, customer.getEmail());
            stmt.setString(5, customer.getAddress());
            stmt.setTimestamp(6, Timestamp.valueOf(customer.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.valueOf(customer.getUpdatedAt()));
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    customer.setCustomerId(rs.getInt(1));
                }
            }
        }
    }
    
    public Customer getCustomerById(int customerId) throws SQLException {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCustomer(rs);
                }
            }
        }
        return null;
    }
    
    public List<Customer> getAllCustomers() throws SQLException {
        String sql = "SELECT * FROM customers ORDER BY company_name";
        List<Customer> customers = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                customers.add(mapResultSetToCustomer(rs));
            }
        }
        return customers;
    }
    
    public List<Customer> searchCustomers(String searchTerm) throws SQLException {
        String sql = "SELECT * FROM customers WHERE company_name LIKE ? OR contact_person LIKE ? OR phone LIKE ? OR email LIKE ? ORDER BY company_name";
        List<Customer> customers = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }
        }
        return customers;
    }
    
    public void updateCustomer(Customer customer) throws SQLException {
        String sql = "UPDATE customers SET company_name = ?, contact_person = ?, phone = ?, email = ?, address = ?, updated_at = ? WHERE customer_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, customer.getCompanyName());
            stmt.setString(2, customer.getContactPerson());
            stmt.setString(3, customer.getPhone());
            stmt.setString(4, customer.getEmail());
            stmt.setString(5, customer.getAddress());
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(7, customer.getCustomerId());
            
            stmt.executeUpdate();
        }
    }
    
    public void deleteCustomer(int customerId) throws SQLException {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.executeUpdate();
        }
    }
    
    public boolean isCompanyNameExists(String companyName, int excludeCustomerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM customers WHERE company_name = ? AND customer_id != ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, companyName);
            stmt.setInt(2, excludeCustomerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
    
    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setCustomerId(rs.getInt("customer_id"));
        customer.setCompanyName(rs.getString("company_name"));
        customer.setContactPerson(rs.getString("contact_person"));
        customer.setPhone(rs.getString("phone"));
        customer.setEmail(rs.getString("email"));
        customer.setAddress(rs.getString("address"));
        customer.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        customer.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return customer;
    }
} 