package com.bicakclc.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.bicakclc.model.Category;
import com.bicakclc.model.Customer;
import com.bicakclc.model.Invoice;
import com.bicakclc.model.InvoiceItem;
import com.bicakclc.model.Product;
import com.bicakclc.service.CategoryService;
import com.bicakclc.service.CustomerService;
import com.bicakclc.service.InvoiceItemService;
import com.bicakclc.service.InvoiceService;
import com.bicakclc.service.ProductService;
import com.bicakclc.util.DatabaseConnection;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.stream.Collectors;

public class InvoicePanel extends JPanel {
    private InvoiceService invoiceService;
    private InvoiceItemService invoiceItemService;
    private ProductService productService;
    private CategoryService categoryService;
    private CustomerService customerService;
    private Connection connection;
    
    // UI Components
    private JTable invoiceTable;
    private DefaultTableModel invoiceTableModel;
    private JComboBox<String> productComboBox;
    private JComboBox<String> categoryComboBox;
    private JTextField companyNameField;
    private JTextField qualityField;
    private JTextField discountField;
    private JTextField laborCostField;
    private JLabel totalAmountLabel;
    private JLabel totalQuantityLabel;
    private JLabel finalAmountLabel;
    private JTextField cmField;
    private JTextField quantityField;
    
    // Autocomplete components
    private JComboBox<String> companyNameComboBox;
    private List<Customer> customerList;
    
    // Data
    private List<Product> productList;
    private List<Category> categoryList;
    private Invoice currentInvoice;
    private List<InvoiceItem> invoiceItems;
    
    private FavoritePanel favoritePanel; // Add this line
    
    public InvoicePanel() {
        setLayout(new BorderLayout());
        initializeServices();
        favoritePanel = new FavoritePanel();
        // Set up the addToInvoiceListener
        favoritePanel.setAddToInvoiceListener(product -> addProductToInvoice(product));
        initializeComponents();
        loadData();
    }
    
    private void initializeServices() {
        try {
            connection = DatabaseConnection.getConnection();
            invoiceService = new InvoiceService(connection);
            invoiceItemService = new InvoiceItemService(connection);
            productService = new ProductService();
            categoryService = new CategoryService();
            customerService = new CustomerService(connection);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Servis başlatılamadı: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void initializeComponents() {
        // Main panel with split layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // Left panel - Product selection and invoice items
        JPanel leftPanel = createLeftPanel();
        
        // Right panel - Invoice details and totals
        JPanel rightPanel = createRightPanel();

        // --- FAVORITE PANEL INTEGRATION ---
        JPanel rightWithFavorites = new JPanel();
        rightWithFavorites.setLayout(new BoxLayout(rightWithFavorites, BoxLayout.X_AXIS));
        rightWithFavorites.add(favoritePanel);
        rightWithFavorites.add(Box.createHorizontalStrut(10));
        rightWithFavorites.add(rightPanel);
        // ----------------------------------

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightWithFavorites);
        splitPane.setDividerLocation(600);
        
        add(splitPane, BorderLayout.CENTER);
    }
    
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Product selection panel
        JPanel selectionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Category filter
        gbc.gridx = 0; gbc.gridy = 0;
        selectionPanel.add(new JLabel("Kategori:"), gbc);
        
        gbc.gridx = 1;
        categoryComboBox = new JComboBox<>();
        categoryComboBox.addItem("Tüm Kategoriler");
        selectionPanel.add(categoryComboBox, gbc);
        
        // Product selection
        gbc.gridx = 0; gbc.gridy = 1;
        selectionPanel.add(new JLabel("Ürün:"), gbc);
        
        gbc.gridx = 1;
        productComboBox = new JComboBox<>();
        selectionPanel.add(productComboBox, gbc);
        
        // Quantity and CM
        gbc.gridx = 0; gbc.gridy = 2;
        selectionPanel.add(new JLabel("Adet:"), gbc);
        
        gbc.gridx = 1;
        quantityField = new JTextField(10);
        selectionPanel.add(quantityField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        selectionPanel.add(new JLabel("CM:"), gbc);
        
        gbc.gridx = 1;
        cmField = new JTextField(10);
        selectionPanel.add(cmField, gbc);
        
        // Add to invoice button
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        JButton addToInvoiceButton = new JButton("Faturaya Ekle");
        addToInvoiceButton.addActionListener(e -> addItemToInvoice());
        selectionPanel.add(addToInvoiceButton, gbc);
        
        panel.add(selectionPanel, BorderLayout.NORTH);
        
        // Invoice items table
        createInvoiceTable();
        JScrollPane scrollPane = new JScrollPane(invoiceTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Table buttons
        JPanel tableButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton removeItemButton = new JButton("Kalemi Sil");
        JButton clearTableButton = new JButton("Tabloyu Temizle");
        
        // Price adjustment section
        JPanel priceAdjustPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        priceAdjustPanel.setBorder(BorderFactory.createTitledBorder("Fiyat Ayarlama"));
        
        JLabel percentageLabel = new JLabel("Yüzde:");
        JSpinner percentageSpinner = new JSpinner(new SpinnerNumberModel(10, -100, 1000, 1));
        percentageSpinner.setPreferredSize(new Dimension(60, 25));
        
        JButton increasePriceButton = new JButton("Fiyat Artır");
        JButton decreasePriceButton = new JButton("Fiyat Azalt");
        
        increasePriceButton.addActionListener(e -> adjustPrices(true, (Integer) percentageSpinner.getValue()));
        decreasePriceButton.addActionListener(e -> adjustPrices(false, (Integer) percentageSpinner.getValue()));
        
        priceAdjustPanel.add(percentageLabel);
        priceAdjustPanel.add(percentageSpinner);
        priceAdjustPanel.add(increasePriceButton);
        priceAdjustPanel.add(decreasePriceButton);
        
        removeItemButton.addActionListener(e -> removeSelectedItem());
        clearTableButton.addActionListener(e -> clearInvoiceTable());
        
        tableButtonPanel.add(removeItemButton);
        tableButtonPanel.add(clearTableButton);
        tableButtonPanel.add(priceAdjustPanel);
        panel.add(tableButtonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Fatura Bilgileri"));
        
        // Invoice details form
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Company name with simple search
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Firma Adı:"), gbc);
        
        gbc.gridx = 1;
        JPanel companyPanel = new JPanel(new BorderLayout());
        companyNameField = new JTextField(15);
        JButton searchButton = new JButton("Ara");
        searchButton.setPreferredSize(new Dimension(60, 25));
        
        searchButton.addActionListener(e -> searchCompany());
        
        // Add Enter key listener for search
        companyNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchCompany();
                }
            }
        });
        
        companyPanel.add(companyNameField, BorderLayout.CENTER);
        companyPanel.add(searchButton, BorderLayout.EAST);
        formPanel.add(companyPanel, gbc);
        
        // Quality
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Kalite:"), gbc);
        
        gbc.gridx = 1;
        qualityField = new JTextField(20);
        formPanel.add(qualityField, gbc);
        
        // Date (read-only)
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Tarih:"), gbc);
        
        gbc.gridx = 1;
        JTextField dateField = new JTextField(20);
        dateField.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        dateField.setEditable(false);
        formPanel.add(dateField, gbc);
        
        // Totals section
        JPanel totalsPanel = new JPanel(new GridBagLayout());
        totalsPanel.setBorder(BorderFactory.createTitledBorder("Tutar Bilgileri"));
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(5, 5, 5, 5);
        
        // Total Quantity
        gbc2.gridx = 0; gbc2.gridy = 0;
        totalsPanel.add(new JLabel("Toplam Adet:"), gbc2);
        
        gbc2.gridx = 1;
        totalQuantityLabel = new JLabel("0");
        totalsPanel.add(totalQuantityLabel, gbc2);
        
        // Total amount
        gbc2.gridx = 0; gbc2.gridy = 1;
        totalsPanel.add(new JLabel("Toplam Tutar:"), gbc2);
        
        gbc2.gridx = 1;
        totalAmountLabel = new JLabel("0.00 TL");
        totalAmountLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalsPanel.add(totalAmountLabel, gbc2);
        
        // Labor Cost
        gbc2.gridx = 0; gbc2.gridy = 2;
        totalsPanel.add(new JLabel("İşçilik Maliyeti:"), gbc2);

        gbc2.gridx = 1;
        laborCostField = new JTextField(15);
        laborCostField.setText("0.00");
        totalsPanel.add(laborCostField, gbc2);
        
        // Discount
        gbc2.gridx = 0; gbc2.gridy = 3;
        totalsPanel.add(new JLabel("İskonto Tutarı:"), gbc2);
        
        gbc2.gridx = 1;
        discountField = new JTextField(15);
        discountField.setText("0.00");
        totalsPanel.add(discountField, gbc2);
        
        // Final amount
        gbc2.gridx = 0; gbc2.gridy = 4;
        totalsPanel.add(new JLabel("Son Tutar:"), gbc2);
        
        gbc2.gridx = 1;
        finalAmountLabel = new JLabel("0.00 TL");
        finalAmountLabel.setFont(new Font("Arial", Font.BOLD, 16));
        finalAmountLabel.setForeground(Color.BLUE);
        totalsPanel.add(finalAmountLabel, gbc2);
        
        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton calculateButton = new JButton("Hesapla");
        JButton saveInvoiceButton = new JButton("Faturayı Kaydet");
        JButton newInvoiceButton = new JButton("Yeni Fatura");
        
        calculateButton.addActionListener(e -> calculateTotals());
        saveInvoiceButton.addActionListener(e -> saveInvoice());
        newInvoiceButton.addActionListener(e -> newInvoice());
        
        buttonPanel.add(calculateButton);
        buttonPanel.add(saveInvoiceButton);
        buttonPanel.add(newInvoiceButton);
        
        // Layout
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(totalsPanel, BorderLayout.CENTER);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(centerPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void createInvoiceTable() {
        String[] columnNames = {"Sıra", "Ürün Adı", "Kategori", "Fiyat", "CM", "Adet", "Toplam"};
        invoiceTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Fiyat (3), CM (4), Adet (5), Toplam (6) düzenlenebilir
                return column == 3 || column == 4 || column == 5 || column == 6;
            }
        };
        invoiceTable = new JTable(invoiceTableModel);
        invoiceTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Add table model listener for automatic calculation
        invoiceTableModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row < 0 || col < 0) return;
            // Eğer toplam dışındaki bir hücre değiştiyse toplamı otomatik güncelle
            if (col == 3 || col == 4 || col == 5) {
                updateRowTotal(row);
            }
            // Toplam değişse bile, kullanıcının girdiği değer bırakılır
            calculateTotals();
        });
    }
    
    private void loadData() {
        try {
            productList = productService.getAllProducts();
            categoryList = categoryService.getAllCategories();
            customerList = customerService.getAllCustomers();
            
            updateCategoryComboBox();
            updateProductComboBox();
            // loadAllCompanyNames() kaldırıldı - artık gerekli değil
            
            newInvoice();
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Veri yüklenemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
        
        // Add listeners
        categoryComboBox.addActionListener(e -> updateProductComboBox());
    }
    
    private void updateCategoryComboBox() {
        categoryComboBox.removeAllItems();
        categoryComboBox.addItem("Tüm Kategoriler");
        for (Category category : categoryList) {
            categoryComboBox.addItem(category.getCategoryName());
        }
    }
    
    private void updateProductComboBox() {
        productComboBox.removeAllItems();
        String selectedCategory = (String) categoryComboBox.getSelectedItem();
        
        for (Product product : productList) {
            if ("Tüm Kategoriler".equals(selectedCategory)) {
                productComboBox.addItem(product.getProductName());
            } else {
                // Find category name for this product
                for (Category category : categoryList) {
                    if (category.getCategoryId() == product.getCategoryId()) {
                        if (category.getCategoryName().equals(selectedCategory)) {
                            productComboBox.addItem(product.getProductName());
                        }
                        break;
                    }
                }
            }
        }
    }
    
    private void searchCompany() {
        String searchText = companyNameField.getText().trim().toLowerCase();
        
        if (searchText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Arama için firma adı yazın.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Find matching companies
        List<String> matches = new ArrayList<>();
        for (Customer customer : customerList) {
            if (customer.getCompanyName().toLowerCase().contains(searchText)) {
                matches.add(customer.getCompanyName());
            }
        }
        
        if (matches.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Bu isimle firma bulunamadı: " + companyNameField.getText(), 
                "Firma Bulunamadı", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        if (matches.size() == 1) {
            // Only one match, auto-fill
            companyNameField.setText(matches.get(0));
            JOptionPane.showMessageDialog(this, 
                "Firma bulundu: " + matches.get(0), 
                "Firma Bulundu", JOptionPane.INFORMATION_MESSAGE);
        } else {
            // Multiple matches, show selection dialog
            String[] options = matches.toArray(new String[0]);
            String selected = (String) JOptionPane.showInputDialog(this,
                "Birden fazla firma bulundu. Seçin:",
                "Firma Seçimi",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
            
            if (selected != null) {
                companyNameField.setText(selected);
            }
        }
    }
    
    private void addItemToInvoice() {
        String selectedProductName = (String) productComboBox.getSelectedItem();
        if (selectedProductName == null) {
            JOptionPane.showMessageDialog(this, "Lütfen bir ürün seçin.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Find selected product
        Product selectedProduct = null;
        for (Product product : productList) {
            if (product.getProductName().equals(selectedProductName)) {
                selectedProduct = product;
                break;
            }
        }
        if (selectedProduct == null) {
            JOptionPane.showMessageDialog(this, "Seçilen ürün bulunamadı.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Get quantity and CM
        int quantity = 1;
        try {
            if (!quantityField.getText().trim().isEmpty()) {
                quantity = Integer.parseInt(quantityField.getText().trim());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Geçersiz adet değeri.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }
        BigDecimal cmValue = null;
        try {
            if (!cmField.getText().trim().isEmpty()) {
                cmValue = new BigDecimal(cmField.getText().trim());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Geçersiz CM değeri.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Create invoice item
        InvoiceItem item = new InvoiceItem();
        item.setProductId(selectedProduct.getProductId());
        item.setProductName(selectedProduct.getProductName());
        item.setPrice(selectedProduct.getPrice());
        item.setQuantity(quantity);
        item.setCmValue(cmValue);
        item.setProductCode(selectedProduct.getProductCode());
        // Calculate total
        calculateItemTotal(item);
        // --- Sıra numarası ve alt grup mantığı ---
        int anaGrupSira = 0;
        int lastAnaGrupIndex = -1;
        // Son eklenen ana grup (productCode=1) satırını bul
        for (int i = invoiceTableModel.getRowCount() - 1; i >= 0; i--) {
            String prevProductName = invoiceTableModel.getValueAt(i, 1).toString();
            Product prevProduct = null;
            for (Product p : productList) {
                if (p.getProductName().equals(prevProductName)) {
                    prevProduct = p;
                    break;
                }
            }
            if (prevProduct != null && "1".equals(prevProduct.getProductCode())) {
                lastAnaGrupIndex = i;
                break;
            }
        }
        // Ana grup sıra numarası kaç?
        for (int i = 0; i < invoiceTableModel.getRowCount(); i++) {
            String prevProductName = invoiceTableModel.getValueAt(i, 1).toString();
            Product prevProduct = null;
            for (Product p : productList) {
                if (p.getProductName().equals(prevProductName)) {
                    prevProduct = p;
                    break;
                }
            }
            if (prevProduct != null && "1".equals(prevProduct.getProductCode())) {
                anaGrupSira++;
            }
        }
        Object rowNumberToShow = null;
        // Ana grup ise sıra numarası ver
        if ("1".equals(selectedProduct.getProductCode())) {
            rowNumberToShow = anaGrupSira + 1;
        } else {
            // Alt grup ve diğer ürünler için sıra numarası boş
            rowNumberToShow = "";
        }
        // Add to table
        String categoryName = "";
        for (Category category : categoryList) {
            if (category.getCategoryId() == selectedProduct.getCategoryId()) {
                categoryName = category.getCategoryName();
                break;
            }
        }
        invoiceTableModel.addRow(new Object[]{
            rowNumberToShow,
            item.getProductName(),
            categoryName,
            item.getPrice(),
            item.getCmValue(),
            item.getQuantity(),
            item.getTotal()
        });
        // Clear input fields
        quantityField.setText("");
        cmField.setText("");
        // Calculate totals
        calculateTotals();
    }
    
    private void calculateItemTotal(InvoiceItem item) {
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
    
    private void updateRowTotal(int row) {
        try {
            java.math.BigDecimal price = new java.math.BigDecimal(invoiceTableModel.getValueAt(row, 3).toString());
            java.math.BigDecimal cmValue = null;
            if (invoiceTableModel.getValueAt(row, 4) != null && !invoiceTableModel.getValueAt(row, 4).toString().isEmpty()) {
                cmValue = new java.math.BigDecimal(invoiceTableModel.getValueAt(row, 4).toString());
            }
            int quantity = Integer.parseInt(invoiceTableModel.getValueAt(row, 5).toString());
            // Alt grupsa üst grubun adedini bul
            Object rowNumber = invoiceTableModel.getValueAt(row, 0);
            boolean isSubGroup = (rowNumber == null || rowNumber.toString().isEmpty());
            int finalQuantity = quantity;
            if (isSubGroup) {
                // En yakın üst grubu bul
                int parentIndex = -1;
                for (int i = row - 1; i >= 0; i--) {
                    Object parentRowNumber = invoiceTableModel.getValueAt(i, 0);
                    if (parentRowNumber != null && !parentRowNumber.toString().isEmpty()) {
                        parentIndex = i;
                        break;
                    }
                }
                if (parentIndex >= 0) {
                    try {
                        int parentQuantity = Integer.parseInt(invoiceTableModel.getValueAt(parentIndex, 5).toString());
                        finalQuantity = parentQuantity * quantity;
                    } catch (Exception ex) { finalQuantity = quantity; }
                }
            }
            java.math.BigDecimal total;
            if (cmValue != null) {
                total = price.multiply(java.math.BigDecimal.valueOf(finalQuantity)).multiply(cmValue);
            } else {
                total = price.multiply(java.math.BigDecimal.valueOf(finalQuantity));
            }
            invoiceTableModel.setValueAt(total, row, 6);
            if (isSubGroup) {
                invoiceTableModel.setValueAt(finalQuantity, row, 5);
            }
        } catch (Exception e) {
            // Ignore calculation errors
        }
    }
    
    private void calculateTotals() {
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        
        for (int i = 0; i < invoiceTableModel.getRowCount(); i++) {
            try {
                BigDecimal rowTotal = new BigDecimal(invoiceTableModel.getValueAt(i, 6).toString());
                totalAmount = totalAmount.add(rowTotal);
                
                Object rowNumber = invoiceTableModel.getValueAt(i, 0);
                if (rowNumber != null && !rowNumber.toString().isEmpty()) {
                    int rowQuantity = Integer.parseInt(invoiceTableModel.getValueAt(i, 5).toString());
                    totalQuantity += rowQuantity;
                }
            } catch (Exception e) {
                // Ignore invalid values
            }
        }
        
        totalAmountLabel.setText(String.format("%.2f TL", totalAmount));
        totalQuantityLabel.setText(String.valueOf(totalQuantity));
        
        // Calculate final amount
        BigDecimal discountAmount = BigDecimal.ZERO;
        try {
            if (!discountField.getText().trim().isEmpty()) {
                discountAmount = new BigDecimal(discountField.getText().trim());
            }
        } catch (NumberFormatException e) {
            discountField.setText("0.00");
        }

        BigDecimal laborCostAmount = BigDecimal.ZERO;
        try {
            if (!laborCostField.getText().trim().isEmpty()) {
                laborCostAmount = new BigDecimal(laborCostField.getText().trim());
            }
        } catch (NumberFormatException e) {
            laborCostField.setText("0.00");
        }
        
        BigDecimal finalAmount = totalAmount.subtract(discountAmount).add(laborCostAmount);
        finalAmountLabel.setText(String.format("%.2f TL", finalAmount));
    }
    
    private void removeSelectedItem() {
        int selectedRow = invoiceTable.getSelectedRow();
        if (selectedRow >= 0) {
            invoiceTableModel.removeRow(selectedRow);
            // Update row numbers
            for (int i = 0; i < invoiceTableModel.getRowCount(); i++) {
                invoiceTableModel.setValueAt(i + 1, i, 0);
            }
            calculateTotals();
        } else {
            JOptionPane.showMessageDialog(this, "Lütfen silmek için bir satır seçin.", "Uyarı", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void clearInvoiceTable() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Tüm fatura kalemlerini silmek istediğinize emin misiniz?", 
            "Onay", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            invoiceTableModel.setRowCount(0);
            calculateTotals();
        }
    }
    
    private void adjustPrices(boolean increase, int percentage) {
        int[] selectedRows = invoiceTable.getSelectedRows();
        
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Lütfen fiyatını değiştirmek istediğiniz ürünleri seçin.", 
                "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (percentage <= 0) {
            JOptionPane.showMessageDialog(this, 
                "Lütfen geçerli bir yüzde değeri girin (0'dan büyük).", 
                "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            for (int row : selectedRows) {
                // Get current price
                BigDecimal currentPrice = new BigDecimal(invoiceTableModel.getValueAt(row, 3).toString());
                
                // Calculate new price
                BigDecimal multiplier = BigDecimal.ONE;
                if (increase) {
                    multiplier = BigDecimal.ONE.add(BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100)));
                } else {
                    multiplier = BigDecimal.ONE.subtract(BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100)));
                }
                
                BigDecimal newPrice = currentPrice.multiply(multiplier).setScale(2, BigDecimal.ROUND_HALF_UP);
                
                // Update price in table
                invoiceTableModel.setValueAt(newPrice, row, 3);
                
                // Recalculate row total
                updateRowTotal(row);
            }
            
            // Recalculate totals
            calculateTotals();
            
            String action = increase ? "artırıldı" : "azaltıldı";
            JOptionPane.showMessageDialog(this, 
                selectedRows.length + " ürünün fiyatı %" + percentage + " " + action + ".", 
                "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Fiyat güncellenirken hata oluştu: " + e.getMessage(), 
                "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void newInvoice() {
        currentInvoice = new Invoice();
        invoiceItems = new ArrayList<>();
        invoiceTableModel.setRowCount(0);
        companyNameField.setText("");  // companyNameComboBox yerine
        qualityField.setText("");
        discountField.setText("0.00");
        laborCostField.setText("0.00");
        calculateTotals();
    }
    
    private String generateInvoiceNumber() {
        // Generate invoice number based on current date and daily sequence
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = now.format(dateFormatter);
        
        try {
            // Get next daily sequence number from database
            int sequence = invoiceService.getNextDailySequence(dateStr);
            System.out.println("Generated sequence: " + sequence + " for date: " + dateStr);
            return String.format("FTR-%s-%03d", dateStr, sequence);
        } catch (SQLException e) {
            System.err.println("Error getting sequence: " + e.getMessage());
            // Fallback: use current time if database error
            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HHmm");
            String timeStr = now.format(timeFormatter);
            int sequence = Integer.parseInt(timeStr);
            System.out.println("Fallback sequence: " + sequence + " for date: " + dateStr);
            return String.format("FTR-%s-%03d", dateStr, sequence);
        }
    }
    
    private void saveInvoice() {
        try {
            // Get company name from text field
            String companyName = companyNameField.getText().trim();
            
            if (companyName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Firma adı boş olamaz!", "Hata", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check if company exists in database
            boolean companyExists = customerList.stream()
                .anyMatch(customer -> customer.getCompanyName().equalsIgnoreCase(companyName));
            
            if (!companyExists) {
                int result = JOptionPane.showConfirmDialog(this, 
                    "Bu firma adı veritabanında bulunmuyor. Yine de devam etmek istiyor musunuz?\n\nFirma: " + companyName, 
                    "Firma Bulunamadı", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            if (invoiceTableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Faturaya en az bir kalem ekleyin.", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Generate invoice number
            String invoiceNumber = generateInvoiceNumber();
            
            // Create invoice
            currentInvoice.setInvoiceNumber(invoiceNumber);
            currentInvoice.setCompanyName(companyName);
            currentInvoice.setQuality(qualityField.getText().trim());
            currentInvoice.setCreatedBy("User"); // TODO: Get actual user
            
            // Calculate totals
            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalQuantity = 0;
            for (int i = 0; i < invoiceTableModel.getRowCount(); i++) {
                BigDecimal rowTotal = new BigDecimal(invoiceTableModel.getValueAt(i, 6).toString());
                totalAmount = totalAmount.add(rowTotal);
                
                int rowQuantity = Integer.parseInt(invoiceTableModel.getValueAt(i, 5).toString());
                totalQuantity += rowQuantity;
            }
            
            BigDecimal discountAmount = BigDecimal.ZERO;
            if (!discountField.getText().trim().isEmpty()) {
                discountAmount = new BigDecimal(discountField.getText().trim());
            }

            BigDecimal laborCostAmount = BigDecimal.ZERO;
            if (!laborCostField.getText().trim().isEmpty()) {
                laborCostAmount = new BigDecimal(laborCostField.getText().trim());
            }
            
            currentInvoice.setTotalAmount(totalAmount);
            currentInvoice.setDiscountAmount(discountAmount);
            currentInvoice.setLaborCostAmount(laborCostAmount);
            currentInvoice.setTotalQuantity(totalQuantity);
            
            // Save invoice
            Invoice savedInvoice = invoiceService.createInvoice(currentInvoice);
            
            // Save invoice items
            for (int i = 0; i < invoiceTableModel.getRowCount(); i++) {
                InvoiceItem item = new InvoiceItem();
                item.setInvoiceId(savedInvoice.getInvoiceId());
                // Find product by name
                String productName = invoiceTableModel.getValueAt(i, 1).toString();
                Product selectedProduct = null;
                for (Product product : productList) {
                    if (product.getProductName().equals(productName)) {
                        item.setProductId(product.getProductId());
                        selectedProduct = product;
                        break;
                    }
                }
                // Set subGroup based on product code
                if (selectedProduct != null) {
                    if (!"1".equals(selectedProduct.getProductCode())) {
                        item.setSubGroup(true);
                    } else {
                        item.setSubGroup(false);
                    }
                }
                // Set other values from table (not from product)
                if (invoiceTableModel.getValueAt(i, 3) != null && !invoiceTableModel.getValueAt(i, 3).toString().isEmpty()) {
                    item.setPrice(new BigDecimal(invoiceTableModel.getValueAt(i, 3).toString()));
                }
                if (invoiceTableModel.getValueAt(i, 4) != null && !invoiceTableModel.getValueAt(i, 4).toString().isEmpty()) {
                    item.setCmValue(new BigDecimal(invoiceTableModel.getValueAt(i, 4).toString()));
                }
                item.setQuantity(Integer.parseInt(invoiceTableModel.getValueAt(i, 5).toString()));
                item.setTotal(new BigDecimal(invoiceTableModel.getValueAt(i, 6).toString()));
                item.setRowNumber(i + 1);
                invoiceItemService.createInvoiceItem(item);
            }
            
            JOptionPane.showMessageDialog(this, 
                "Fatura başarıyla kaydedildi.\nFatura No: " + savedInvoice.getInvoiceId(), 
                "Başarılı", JOptionPane.INFORMATION_MESSAGE);
            
            newInvoice();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Fatura kaydedilemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- FAVORITE PANEL SYNCHRONIZATION ---
    // Static reference for cross-panel refresh
    private static java.util.List<FavoritePanel> favoritePanelObservers = new java.util.ArrayList<>();
    static void registerFavoritePanel(FavoritePanel panel) {
        if (!favoritePanelObservers.contains(panel)) favoritePanelObservers.add(panel);
    }
    static void refreshAllFavoritePanels() {
        for (FavoritePanel panel : favoritePanelObservers) {
            panel.refresh();
        }
    }
    public void refreshFavorites() {
        favoritePanel.refresh();
    }

    // Add this method to handle adding a product from favorites
    public void addProductToInvoice(Product product) {
        // 1. Find all main group rows (Çelik, product_code=1) in the current invoice table
        java.util.List<String> mainGroups = new java.util.ArrayList<>();
        java.util.List<Integer> mainGroupQuantities = new java.util.ArrayList<>();
        for (int i = 0; i < invoiceTableModel.getRowCount(); i++) {
            Object productName = invoiceTableModel.getValueAt(i, 1);
            Object category = invoiceTableModel.getValueAt(i, 2);
            Object productCode = null;
            for (Product p : productList) {
                if (p.getProductName().equals(productName)) {
                    productCode = p.getProductCode();
                    break;
                }
            }
            if ("1".equals(productCode)) {
                mainGroups.add(productName.toString());
                try {
                    mainGroupQuantities.add(Integer.parseInt(invoiceTableModel.getValueAt(i, 5).toString()));
                } catch (Exception e) { mainGroupQuantities.add(1); }
            }
        }
        // 2. Show dialog for group selection and quantity/cm
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Adet:"));
        JTextField quantityField = new JTextField("1");
        panel.add(quantityField);
        panel.add(new JLabel("CM (opsiyonel):"));
        JTextField cmField = new JTextField("");
        panel.add(cmField);
        JComboBox<String> groupCombo = null;
        if (!mainGroups.isEmpty() && !"1".equals(product.getProductCode())) {
            panel.add(new JLabel("Alt grup olarak ekle:"));
            groupCombo = new JComboBox<>();
            for (String mg : mainGroups) groupCombo.addItem(mg);
            groupCombo.addItem("Bağımsız");
            panel.add(groupCombo);
        }
        int result = JOptionPane.showConfirmDialog(this, panel, "Favori Ürünü Faturaya Ekle", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int quantity = 1;
            try { quantity = Integer.parseInt(quantityField.getText().trim()); } catch (Exception e) {}
            java.math.BigDecimal cmValue = null;
            try { String cmText = cmField.getText().trim(); if (!cmText.isEmpty()) cmValue = new java.math.BigDecimal(cmText); } catch (Exception e) {}
            // Determine if this is a sub-group or main group
            boolean isSubGroup = false;
            int parentGroupIndex = -1;
            if (groupCombo != null && groupCombo.getSelectedIndex() < groupCombo.getItemCount() - 1) {
                isSubGroup = true;
                parentGroupIndex = groupCombo.getSelectedIndex();
            }
            // Add to invoice table
            String categoryName = "";
            for (Category cat : categoryList) {
                if (cat.getCategoryId() == product.getCategoryId()) {
                    categoryName = cat.getCategoryName();
                    break;
                }
            }
            Object rowNumberToShow = invoiceTableModel.getRowCount() + 1;
            if (isSubGroup) rowNumberToShow = "";
            java.math.BigDecimal price = product.getPrice();
            java.math.BigDecimal total;
            int finalQuantity = quantity;
            if (isSubGroup && parentGroupIndex >= 0) {
                // Alt grup: üst grubun adedi ile çarp
                int parentQuantity = mainGroupQuantities.get(parentGroupIndex);
                finalQuantity = parentQuantity * quantity;
                if (cmValue != null) {
                    total = price.multiply(java.math.BigDecimal.valueOf(finalQuantity)).multiply(cmValue);
                } else {
                    total = price.multiply(java.math.BigDecimal.valueOf(finalQuantity));
                }
            } else {
                // Ana grup veya bağımsız ürün
                if (cmValue != null) {
                    total = price.multiply(java.math.BigDecimal.valueOf(quantity)).multiply(cmValue);
                } else {
                    total = price.multiply(java.math.BigDecimal.valueOf(quantity));
                }
            }
            invoiceTableModel.addRow(new Object[]{
                rowNumberToShow,
                product.getProductName(),
                categoryName,
                price,
                cmValue,
                finalQuantity,
                total
            });
            calculateTotals();
        }
    }
} 