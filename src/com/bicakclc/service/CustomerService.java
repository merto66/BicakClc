package com.bicakclc.service;

import com.bicakclc.dao.CustomerDAO;
import com.bicakclc.model.Customer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class CustomerService {
    private CustomerDAO customerDAO;
    private Connection connection;
    
    public CustomerService(Connection connection) {
        this.connection = connection;
        this.customerDAO = new CustomerDAO(connection);
    }
    
    public void createCustomer(Customer customer) throws SQLException {
        // Validation
        validateCustomer(customer);
        
        // Check if company name already exists
        if (customerDAO.isCompanyNameExists(customer.getCompanyName(), 0)) {
            throw new SQLException("Bu firma adı zaten mevcut: " + customer.getCompanyName());
        }
        
        customerDAO.createCustomer(customer);
    }
    
    public Customer getCustomerById(int customerId) throws SQLException {
        return customerDAO.getCustomerById(customerId);
    }
    
    public List<Customer> getAllCustomers() throws SQLException {
        return customerDAO.getAllCustomers();
    }
    
    public List<Customer> searchCustomers(String searchTerm) throws SQLException {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllCustomers();
        }
        return customerDAO.searchCustomers(searchTerm.trim());
    }
    
    public void updateCustomer(Customer customer) throws SQLException {
        // Validation
        validateCustomer(customer);
        
        // Check if company name already exists (excluding current customer)
        if (customerDAO.isCompanyNameExists(customer.getCompanyName(), customer.getCustomerId())) {
            throw new SQLException("Bu firma adı zaten mevcut: " + customer.getCompanyName());
        }
        
        customerDAO.updateCustomer(customer);
    }
    
    public void deleteCustomer(int customerId) throws SQLException {
        // Check if customer exists
        Customer customer = customerDAO.getCustomerById(customerId);
        if (customer == null) {
            throw new SQLException("Müşteri bulunamadı: " + customerId);
        }
        
        // TODO: Check if customer has any invoices before deleting
        // For now, we'll allow deletion
        
        customerDAO.deleteCustomer(customerId);
    }
    
    private void validateCustomer(Customer customer) throws SQLException {
        if (customer.getCompanyName() == null || customer.getCompanyName().trim().isEmpty()) {
            throw new SQLException("Firma adı boş olamaz");
        }
        
        if (customer.getCompanyName().trim().length() < 2) {
            throw new SQLException("Firma adı en az 2 karakter olmalıdır");
        }
        
        if (customer.getCompanyName().trim().length() > 100) {
            throw new SQLException("Firma adı en fazla 100 karakter olabilir");
        }
        
        // Optional validations
        if (customer.getPhone() != null && !customer.getPhone().trim().isEmpty()) {
            if (customer.getPhone().trim().length() < 10) {
                throw new SQLException("Telefon numarası en az 10 karakter olmalıdır");
            }
        }
        
        if (customer.getEmail() != null && !customer.getEmail().trim().isEmpty()) {
            if (!customer.getEmail().contains("@")) {
                throw new SQLException("Geçerli bir e-posta adresi giriniz");
            }
        }
    }
} 