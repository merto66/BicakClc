package com.bicakclc.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.bicakclc.model.Customer;
import com.bicakclc.service.CustomerService;
import com.bicakclc.util.DatabaseConnection;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.sql.Connection;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CustomerPanel extends JPanel {
    private CustomerService customerService;
    private Connection connection;
    
    // UI Components
    private JTable customerTable;
    private DefaultTableModel customerTableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private JTextField searchField;
    private JButton searchButton;
    private JButton clearSearchButton;
    
    // Form fields
    private JTextField companyNameField;
    private JTextField contactPersonField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextArea addressArea;
    
    // Data
    private List<Customer> customerList;
    private Customer selectedCustomer;
    
    public CustomerPanel() {
        setLayout(new BorderLayout());
        initializeServices();
        initializeComponents();
        loadData();
    }
    
    private void initializeServices() {
        try {
            connection = DatabaseConnection.getConnection();
            customerService = new CustomerService(connection);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Servis başlatılamadı: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void initializeComponents() {
        // Top panel - Search and buttons
        JPanel topPanel = createTopPanel();
        
        // Center panel - Customer table
        createCustomerTable();
        JScrollPane scrollPane = new JScrollPane(customerTable);
        
        // Layout
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Müşteri İşlemleri"));
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        searchField.setToolTipText("Firma adı, kişi, telefon veya e-posta ile arama yapın");
        searchButton = new JButton("Ara");
        clearSearchButton = new JButton("Temizle");
        
        searchButton.addActionListener(e -> searchCustomers());
        clearSearchButton.addActionListener(e -> clearSearch());
        
        searchPanel.add(new JLabel("Arama:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearSearchButton);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton = new JButton("Yeni Müşteri");
        editButton = new JButton("Düzenle");
        deleteButton = new JButton("Sil");
        refreshButton = new JButton("Yenile");
        
        addButton.addActionListener(e -> showCustomerDialog(null));
        editButton.addActionListener(e -> editSelectedCustomer());
        deleteButton.addActionListener(e -> deleteSelectedCustomer());
        refreshButton.addActionListener(e -> loadData());
        
        buttonsPanel.add(addButton);
        buttonsPanel.add(editButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(refreshButton);
        
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(buttonsPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void createCustomerTable() {
        String[] columnNames = {"ID", "Firma Adı", "İletişim Kişisi", "Telefon", "E-posta", "Adres", "Oluşturulma Tarihi", "Güncellenme Tarihi"};
        customerTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only table
            }
        };
        
        customerTable = new JTable(customerTableModel);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add double-click listener for editing
        customerTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedCustomer();
                }
            }
        });
        
        // Add table sorter
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(customerTableModel);
        customerTable.setRowSorter(sorter);
    }
    
    private void loadData() {
        try {
            customerList = customerService.getAllCustomers();
            refreshTable();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Veri yüklenemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refreshTable() {
        customerTableModel.setRowCount(0);
        
        for (Customer customer : customerList) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            String formattedCreatedDate = customer.getCreatedAt().format(formatter);
            String formattedUpdatedDate = customer.getUpdatedAt().format(formatter);
            
            // Truncate address if too long
            String address = customer.getAddress();
            if (address != null && address.length() > 50) {
                address = address.substring(0, 47) + "...";
            }
            
            customerTableModel.addRow(new Object[]{
                customer.getCustomerId(),
                customer.getCompanyName(),
                customer.getContactPerson(),
                customer.getPhone(),
                customer.getEmail(),
                address,
                formattedCreatedDate,
                formattedUpdatedDate
            });
        }
    }
    
    private void searchCustomers() {
        String searchTerm = searchField.getText().trim();
        try {
            customerList = customerService.searchCustomers(searchTerm);
            refreshTable();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Arama yapılamadı: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void clearSearch() {
        searchField.setText("");
        loadData();
    }
    
    private void editSelectedCustomer() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Lütfen düzenlemek için bir müşteri seçin.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Convert to model index if table is sorted
        int modelRow = customerTable.convertRowIndexToModel(selectedRow);
        int customerId = (Integer) customerTableModel.getValueAt(modelRow, 0);
        
        try {
            Customer customer = customerService.getCustomerById(customerId);
            if (customer != null) {
                showCustomerDialog(customer);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Müşteri bilgileri yüklenemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteSelectedCustomer() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Lütfen silmek için bir müşteri seçin.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Convert to model index if table is sorted
        int modelRow = customerTable.convertRowIndexToModel(selectedRow);
        int customerId = (Integer) customerTableModel.getValueAt(modelRow, 0);
        String companyName = customerTableModel.getValueAt(modelRow, 1).toString();
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Bu müşteriyi silmek istediğinizden emin misiniz?\n\nFirma: " + companyName, 
            "Müşteri Silme", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                customerService.deleteCustomer(customerId);
                JOptionPane.showMessageDialog(this, "Müşteri başarıyla silindi.", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Müşteri silinemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void showCustomerDialog(Customer customer) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            customer == null ? "Yeni Müşteri" : "Müşteri Düzenle", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Müşteri Bilgileri"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Initialize form fields
        companyNameField = new JTextField(20);
        contactPersonField = new JTextField(20);
        phoneField = new JTextField(20);
        emailField = new JTextField(20);
        addressArea = new JTextArea(4, 20);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        JScrollPane addressScrollPane = new JScrollPane(addressArea);
        
        // Populate fields if editing
        if (customer != null) {
            companyNameField.setText(customer.getCompanyName());
            contactPersonField.setText(customer.getContactPerson());
            phoneField.setText(customer.getPhone());
            emailField.setText(customer.getEmail());
            addressArea.setText(customer.getAddress());
        }
        
        // Add form components
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Firma Adı *:"), gbc);
        gbc.gridx = 1;
        formPanel.add(companyNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("İletişim Kişisi:"), gbc);
        gbc.gridx = 1;
        formPanel.add(contactPersonField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Telefon:"), gbc);
        gbc.gridx = 1;
        formPanel.add(phoneField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("E-posta:"), gbc);
        gbc.gridx = 1;
        formPanel.add(emailField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Adres:"), gbc);
        gbc.gridx = 1;
        formPanel.add(addressScrollPane, gbc);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveButton = new JButton("Kaydet");
        JButton cancelButton = new JButton("İptal");
        
        saveButton.addActionListener(e -> {
            if (saveCustomer(customer)) {
                dialog.dispose();
                loadData();
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonsPanel.add(saveButton);
        buttonsPanel.add(cancelButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonsPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private boolean saveCustomer(Customer customer) {
        try {
            String companyName = companyNameField.getText().trim();
            String contactPerson = contactPersonField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressArea.getText().trim();
            
            if (companyName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Firma adı boş olamaz!", "Hata", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            if (customer == null) {
                // Create new customer
                Customer newCustomer = new Customer(companyName, contactPerson, phone, email, address);
                customerService.createCustomer(newCustomer);
                JOptionPane.showMessageDialog(this, "Müşteri başarıyla oluşturuldu.", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Update existing customer
                customer.setCompanyName(companyName);
                customer.setContactPerson(contactPerson);
                customer.setPhone(phone);
                customer.setEmail(email);
                customer.setAddress(address);
                customerService.updateCustomer(customer);
                JOptionPane.showMessageDialog(this, "Müşteri başarıyla güncellendi.", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
            }
            
            return true;
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Müşteri kaydedilemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
} 