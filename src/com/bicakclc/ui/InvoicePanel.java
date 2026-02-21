package com.bicakclc.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
import java.sql.SQLException;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.math.BigDecimal;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

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
    private JTextField discountPercentageField;
    private JTextField discountField;
    private JTextField laborCostField;
    private JLabel totalAmountLabel;
    private JLabel totalQuantityLabel;
    private JLabel finalAmountLabel;
    private JTextField cmField;
    private JTextField quantityField;
    
    private JWindow companyPopup;
    private JList<Customer> companyList;
    private DefaultListModel<Customer> companyListModel;
    private javax.swing.Timer companySearchTimer;
    private boolean skipNextCompanySearch;
    
    private List<Product> productList;
    private List<Category> categoryList;
    private Invoice currentInvoice;
    
    private FavoritePanel favoritePanel;
    
    public InvoicePanel() {
        setLayout(new BorderLayout());
        initializeServices();
        favoritePanel = new FavoritePanel();
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
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Firma Adı:"), gbc);
        gbc.gridx = 1;
        companyNameField = new JTextField(20);
        companyNameField.setToolTipText("Firma adı yazın, liste otomatik açılır");
        companyListModel = new DefaultListModel<>();
        companyList = new JList<>(companyListModel);
        companyList.setVisibleRowCount(8);
        companyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        companyList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel l = new JLabel(value == null ? "" : ((Customer) value).getCompanyName());
            if (isSelected) {
                l.setBackground(list.getSelectionBackground());
                l.setForeground(list.getSelectionForeground());
                l.setOpaque(true);
            }
            return l;
        });
        companyList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() >= 1) {
                    Customer c = companyList.getSelectedValue();
                    if (c != null) onCompanySelected(c);
                }
            }
        });
        companyList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    Customer c = companyList.getSelectedValue();
                    if (c != null) onCompanySelected(c);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hideCompanyPopup();
                }
            }
        });
        companyPopup = null;
        companySearchTimer = new javax.swing.Timer(280, null);
        companySearchTimer.setRepeats(false);
        companySearchTimer.addActionListener(e -> triggerCompanySearch());
        companyNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { companySearchTimer.restart(); }
            @Override
            public void removeUpdate(DocumentEvent e) { companySearchTimer.restart(); }
            @Override
            public void changedUpdate(DocumentEvent e) { companySearchTimer.restart(); }
        });
        companyNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (companyPopup == null || !companyPopup.isVisible()) return;
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    int i = Math.min(companyList.getSelectedIndex() + 1, companyListModel.getSize() - 1);
                    if (i >= 0) { companyList.setSelectedIndex(i); companyList.ensureIndexIsVisible(i); }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int i = Math.max(companyList.getSelectedIndex() - 1, 0);
                    companyList.setSelectedIndex(i);
                    companyList.ensureIndexIsVisible(i);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    Customer c = companyList.getSelectedValue();
                    if (c != null) onCompanySelected(c);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hideCompanyPopup();
                    e.consume();
                }
            }
        });
        formPanel.add(companyNameField, gbc);
        
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
        
        // Discount Percentage
        gbc2.gridx = 0; gbc2.gridy = 3;
        totalsPanel.add(new JLabel("İskonto (%):"), gbc2);
        
        gbc2.gridx = 1;
        discountPercentageField = new JTextField(15);
        discountPercentageField.setText("0");
        totalsPanel.add(discountPercentageField, gbc2);
        
        // Discount Amount
        gbc2.gridx = 0; gbc2.gridy = 4;
        totalsPanel.add(new JLabel("İskonto Tutarı:"), gbc2);
        
        gbc2.gridx = 1;
        discountField = new JTextField(15);
        discountField.setText("0.00");
        totalsPanel.add(discountField, gbc2);
        
        // Final amount
        gbc2.gridx = 0; gbc2.gridy = 5;
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
        String[] columnNames = {"Sıra", "Ürün Adı", "Kategori", "Fiyat", "CM", "Adet", "İşçilik", "Toplam"};
        invoiceTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column == 6) {
                    Object rowNumber = getValueAt(row, 0);
                    return rowNumber != null && !rowNumber.toString().isEmpty();
                }
                return column == 3 || column == 4 || column == 5 || column == 7;
            }
        };
        invoiceTable = new JTable(invoiceTableModel);
        invoiceTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        invoiceTableModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row < 0 || col < 0) return;
            if (col == 3 || col == 4 || col == 5 || col == 6) {
                updateRowTotal(row);
            }
            calculateTotals();
        });
    }
    
    private void loadData() {
        try {
            productList = productService.getAllProducts();
            categoryList = categoryService.getAllCategories();
            updateCategoryComboBox();
            updateProductComboBox();
            
            newInvoice();
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Veri yüklenemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
        
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
    
    private void triggerCompanySearch() {
        if (skipNextCompanySearch) {
            skipNextCompanySearch = false;
            hideCompanyPopup();
            return;
        }
        String term = companyNameField.getText().trim();
        if (term.length() < 2) {
            hideCompanyPopup();
            if (currentInvoice != null) currentInvoice.setCompanyId(null);
            return;
        }
        if (currentInvoice != null) currentInvoice.setCompanyId(null);
        new SwingWorker<List<Customer>, Void>() {
            @Override
            protected List<Customer> doInBackground() throws Exception {
                return customerService.searchCustomersByCompanyName(term);
            }
            @Override
            protected void done() {
                try {
                    showCompanyPopup(get());
                } catch (Exception e) {
                    hideCompanyPopup();
                }
            }
        }.execute();
    }

    private void showCompanyPopup(List<Customer> customers) {
        if (customers == null || customers.isEmpty()) {
            hideCompanyPopup();
            return;
        }
        if (companyPopup == null) {
            Window w = SwingUtilities.getWindowAncestor(companyNameField);
            companyPopup = new JWindow(w);
            companyPopup.getContentPane().add(new JScrollPane(companyList));
            companyPopup.setFocusableWindowState(false);
        }
        companyListModel.clear();
        for (Customer c : customers) companyListModel.addElement(c);
        companyList.setSelectedIndex(0);
        companyList.ensureIndexIsVisible(0);
        companyPopup.pack();
        companyPopup.setSize(Math.max(companyNameField.getWidth(), 200), Math.min(companyList.getPreferredSize().height + 20, 220));
        companyPopup.setLocation(companyNameField.getLocationOnScreen().x, companyNameField.getLocationOnScreen().y + companyNameField.getHeight());
        companyPopup.setVisible(true);
        companyList.requestFocusInWindow();
    }

    private void hideCompanyPopup() {
        if (companyPopup != null) companyPopup.setVisible(false);
        companyNameField.requestFocusInWindow();
    }

    private void onCompanySelected(Customer c) {
        companySearchTimer.stop();
        skipNextCompanySearch = true;
        companyNameField.setText(c.getCompanyName());
        if (currentInvoice != null) {
            currentInvoice.setCompanyName(c.getCompanyName());
            currentInvoice.setCompanyId(c.getCustomerId());
        }
        hideCompanyPopup();
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
        item.setCmValue(cmValue);
        item.setProductCode(selectedProduct.getProductCode());
        
        int finalQuantity = quantity;
        if (!"1".equals(selectedProduct.getProductCode())) {
            int parentQuantity = 1;
            for (int i = invoiceTableModel.getRowCount() - 1; i >= 0; i--) {
                Object prevRowNumber = invoiceTableModel.getValueAt(i, 0);
                if (prevRowNumber != null && !prevRowNumber.toString().isEmpty()) {
                    try {
                        parentQuantity = Integer.parseInt(invoiceTableModel.getValueAt(i, 5).toString());
                    } catch (Exception ex) {
                        parentQuantity = 1;
                    }
                    break;
                }
            }
            finalQuantity = quantity * parentQuantity;
        }
        
        item.setQuantity(finalQuantity);
        // Calculate total
        calculateItemTotal(item);
        int anaGrupSira = 0;
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
        if ("1".equals(selectedProduct.getProductCode())) {
            rowNumberToShow = anaGrupSira + 1;
        } else {
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
        
        BigDecimal laborCost = BigDecimal.ZERO;
        invoiceTableModel.addRow(new Object[]{
            rowNumberToShow,
            item.getProductName(),
            categoryName,
            item.getPrice(),
            item.getCmValue(),
            item.getQuantity(),
            laborCost,
            item.getTotal()
        });
        quantityField.setText("");
        cmField.setText("");
        calculateTotals();
    }
    
    private void calculateItemTotal(InvoiceItem item) {
        BigDecimal total;
        if (item.getCmValue() != null) {
            total = item.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .multiply(item.getCmValue());
        } else {
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
            Object rowNumber = invoiceTableModel.getValueAt(row, 0);
            boolean isSubGroup = (rowNumber == null || rowNumber.toString().isEmpty());
            int finalQuantity = quantity;
            if (isSubGroup) {
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
            java.math.BigDecimal laborCost = parseLaborCostFromRow(row);
            total = total.add(laborCost);
            invoiceTableModel.setValueAt(total, row, 7);
            if (isSubGroup) {
                invoiceTableModel.setValueAt(finalQuantity, row, 5);
            }
        } catch (Exception e) {
        }
    }

    private BigDecimal parseLaborCostFromRow(int row) {
        Object laborValue = invoiceTableModel.getValueAt(row, 6);
        if (laborValue == null) {
            return BigDecimal.ZERO;
        }
        String laborStr = laborValue.toString()
            .replace(",", ".")
            .replace("TL", "")
            .trim();
        if (laborStr.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(laborStr);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
    
    private void calculateTotals() {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalLaborCost = BigDecimal.ZERO;
        int totalQuantity = 0;
        
        for (int i = 0; i < invoiceTableModel.getRowCount(); i++) {
            try {
                String totalStr = invoiceTableModel.getValueAt(i, 7).toString().replace(",", ".");
                BigDecimal rowTotal = new BigDecimal(totalStr);
                
                Object rowNumber = invoiceTableModel.getValueAt(i, 0);
                BigDecimal rowLabor = BigDecimal.ZERO;
                if (rowNumber != null && !rowNumber.toString().isEmpty()) {
                    int rowQuantity = Integer.parseInt(invoiceTableModel.getValueAt(i, 5).toString());
                    totalQuantity += rowQuantity;
                    
                    Object laborValue = invoiceTableModel.getValueAt(i, 6);
                    if (laborValue != null && !laborValue.toString().isEmpty()) {
                        try {
                            String laborStr = laborValue.toString()
                                .replace(",", ".")
                                .replace("TL", "")
                                .trim();
                            rowLabor = new BigDecimal(laborStr);
                            totalLaborCost = totalLaborCost.add(rowLabor);
                        } catch (NumberFormatException ex) {
                        }
                    }
                }
                totalAmount = totalAmount.add(rowTotal.subtract(rowLabor));
            } catch (Exception e) {
            }
        }
        
        totalAmountLabel.setText(String.format("%.2f TL", totalAmount));
        totalQuantityLabel.setText(String.valueOf(totalQuantity));
        
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal discountPercentage = BigDecimal.ZERO;
        
        try {
            String percentText = discountPercentageField.getText().trim().replace(",", ".");
            if (!percentText.isEmpty()) {
                discountPercentage = new BigDecimal(percentText);
                if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    discountAmount = totalAmount.multiply(discountPercentage)
                                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    discountField.setText(String.format("%.2f", discountAmount));
                }
            } else {
                discountPercentageField.setText("0");
            }
        } catch (NumberFormatException e) {
            discountPercentageField.setText("0");
        }
        
        try {
            String amountText = discountField.getText().trim().replace(",", ".");
            if (!amountText.isEmpty()) {
                BigDecimal directAmount = new BigDecimal(amountText);
                if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal calculatedPercent = directAmount.multiply(BigDecimal.valueOf(100))
                                                    .divide(totalAmount, 2, java.math.RoundingMode.HALF_UP);
                    if (!calculatedPercent.equals(discountPercentage)) {
                        discountPercentageField.setText(String.format("%.2f", calculatedPercent));
                    }
                }
                discountAmount = directAmount;
            } else {
                discountField.setText("0.00");
            }
        } catch (NumberFormatException e) {
            discountField.setText("0.00");
        }

        laborCostField.setText(String.format("%.2f", totalLaborCost));
        laborCostField.setEditable(false);
        
        BigDecimal finalAmount = totalAmount.subtract(discountAmount).add(totalLaborCost);
        finalAmountLabel.setText(String.format("%.2f TL", finalAmount));
    }
    
    private void removeSelectedItem() {
        int selectedRow = invoiceTable.getSelectedRow();
        if (selectedRow >= 0) {
            invoiceTableModel.removeRow(selectedRow);
            
            // Recalculate row numbers for main groups only
            int mainGroupSeq = 0;
            for (int i = 0; i < invoiceTableModel.getRowCount(); i++) {
                String productName = invoiceTableModel.getValueAt(i, 1).toString();
                Product product = null;
                for (Product p : productList) {
                    if (p.getProductName().equals(productName)) {
                        product = p;
                        break;
                    }
                }
                if (product != null && "1".equals(product.getProductCode())) {
                    mainGroupSeq++;
                    invoiceTableModel.setValueAt(mainGroupSeq, i, 0);
                } else {
                    invoiceTableModel.setValueAt("", i, 0);
                }
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
                
                BigDecimal newPrice = currentPrice.multiply(multiplier).setScale(2, java.math.RoundingMode.HALF_UP);
                
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
        invoiceTableModel.setRowCount(0);
        companyNameField.setText("");
        qualityField.setText("");
        discountPercentageField.setText("0");
        discountField.setText("0.00");
        laborCostField.setText("0.00");
        calculateTotals();
    }
    
    private String generateInvoiceNumber() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = now.format(dateFormatter);
        
        try {
            int sequence = invoiceService.getNextDailySequence(dateStr);
            System.out.println("Generated sequence: " + sequence + " for date: " + dateStr);
            return String.format("FTR-%s-%03d", dateStr, sequence);
        } catch (SQLException e) {
            System.err.println("Error getting sequence: " + e.getMessage());
            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HHmm");
            String timeStr = now.format(timeFormatter);
            int sequence = Integer.parseInt(timeStr);
            System.out.println("Fallback sequence: " + sequence + " for date: " + dateStr);
            return String.format("FTR-%s-%03d", dateStr, sequence);
        }
    }
    
    private void saveInvoice() {
        try {
            String companyName = companyNameField.getText().trim();
            
            if (companyName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Firma adı boş olamaz!", "Hata", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            boolean companyExists = customerService.isCompanyNameExists(companyName);
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
            
            String invoiceNumber = generateInvoiceNumber();
            
            currentInvoice.setInvoiceNumber(invoiceNumber);
            currentInvoice.setCompanyName(companyName);
            currentInvoice.setQuality(qualityField.getText().trim());
            currentInvoice.setCreatedBy("User");
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalQuantity = 0;
            for (int i = 0; i < invoiceTableModel.getRowCount(); i++) {
                try {
                    String totalStr = invoiceTableModel.getValueAt(i, 7).toString()
                        .replace(",", ".")
                        .replace("TL", "")
                        .trim();
                    BigDecimal rowTotal = new BigDecimal(totalStr);
                    
                    Object rowNumber = invoiceTableModel.getValueAt(i, 0);
                    BigDecimal rowLabor = BigDecimal.ZERO;
                    if (rowNumber != null && !rowNumber.toString().isEmpty()) {
                        int rowQuantity = Integer.parseInt(invoiceTableModel.getValueAt(i, 5).toString());
                        totalQuantity += rowQuantity;
                        rowLabor = parseLaborCostFromRow(i);
                    }
                    totalAmount = totalAmount.add(rowTotal.subtract(rowLabor));
                } catch (Exception e) {
                }
            }
            
            BigDecimal discountPercentage = BigDecimal.ZERO;
            if (!discountPercentageField.getText().trim().isEmpty()) {
                try {
                    String text = discountPercentageField.getText().trim().replace(",", ".");
                    discountPercentage = new BigDecimal(text);
                } catch (NumberFormatException e) {
                    discountPercentage = BigDecimal.ZERO;
                }
            }
            
            BigDecimal discountAmount = BigDecimal.ZERO;
            if (!discountField.getText().trim().isEmpty()) {
                try {
                    String text = discountField.getText().trim().replace(",", ".");
                    discountAmount = new BigDecimal(text);
                } catch (NumberFormatException e) {
                    discountAmount = BigDecimal.ZERO;
                }
            }

            BigDecimal laborCostAmount = BigDecimal.ZERO;
            if (!laborCostField.getText().trim().isEmpty()) {
                try {
                    String text = laborCostField.getText().trim().replace(",", ".");
                    laborCostAmount = new BigDecimal(text);
                } catch (NumberFormatException e) {
                    laborCostAmount = BigDecimal.ZERO;
                }
            }
            
            currentInvoice.setTotalAmount(totalAmount);
            currentInvoice.setDiscountPercentage(discountPercentage);
            currentInvoice.setDiscountAmount(discountAmount);
            currentInvoice.setLaborCostAmount(laborCostAmount);
            currentInvoice.setTotalQuantity(totalQuantity);
            
            Invoice savedInvoice = invoiceService.createInvoice(currentInvoice);
            
            for (int i = 0; i < invoiceTableModel.getRowCount(); i++) {
                InvoiceItem item = new InvoiceItem();
                item.setInvoiceId(savedInvoice.getInvoiceId());
                String productName = invoiceTableModel.getValueAt(i, 1).toString();
                Product selectedProduct = null;
                for (Product product : productList) {
                    if (product.getProductName().equals(productName)) {
                        item.setProductId(product.getProductId());
                        selectedProduct = product;
                        break;
                    }
                }
                if (selectedProduct != null) {
                    if (!"1".equals(selectedProduct.getProductCode())) {
                        item.setSubGroup(true);
                    } else {
                        item.setSubGroup(false);
                    }
                }
                if (invoiceTableModel.getValueAt(i, 3) != null && !invoiceTableModel.getValueAt(i, 3).toString().isEmpty()) {
                    String priceStr = invoiceTableModel.getValueAt(i, 3).toString().replace(",", ".");
                    item.setPrice(new BigDecimal(priceStr));
                }
                if (invoiceTableModel.getValueAt(i, 4) != null && !invoiceTableModel.getValueAt(i, 4).toString().isEmpty()) {
                    String cmStr = invoiceTableModel.getValueAt(i, 4).toString().replace(",", ".");
                    item.setCmValue(new BigDecimal(cmStr));
                }
                item.setQuantity(Integer.parseInt(invoiceTableModel.getValueAt(i, 5).toString()));
                
                if (invoiceTableModel.getValueAt(i, 6) != null && !invoiceTableModel.getValueAt(i, 6).toString().isEmpty()) {
                    try {
                        String laborStr = invoiceTableModel.getValueAt(i, 6).toString().replace(",", ".").replace("TL", "").trim();
                        item.setLaborCost(new BigDecimal(laborStr));
                    } catch (NumberFormatException e) {
                        item.setLaborCost(BigDecimal.ZERO);
                    }
                } else {
                    item.setLaborCost(BigDecimal.ZERO);
                }
                
                String totalStr = invoiceTableModel.getValueAt(i, 7).toString().replace(",", ".").replace("TL", "").trim();
                BigDecimal displayedTotal = new BigDecimal(totalStr);
                BigDecimal rowLabor = BigDecimal.ZERO;
                Object rowNumber = invoiceTableModel.getValueAt(i, 0);
                if (rowNumber != null && !rowNumber.toString().isEmpty()) {
                    rowLabor = parseLaborCostFromRow(i);
                }
                item.setTotal(displayedTotal.subtract(rowLabor));
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

    public void addProductToInvoice(Product product) {
        java.util.List<String> mainGroups = new java.util.ArrayList<>();
        java.util.List<Integer> mainGroupQuantities = new java.util.ArrayList<>();
        for (int i = 0; i < invoiceTableModel.getRowCount(); i++) {
            Object productName = invoiceTableModel.getValueAt(i, 1);
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
            boolean isSubGroup = false;
            int parentGroupIndex = -1;
            if (groupCombo != null && groupCombo.getSelectedIndex() < groupCombo.getItemCount() - 1) {
                isSubGroup = true;
                parentGroupIndex = groupCombo.getSelectedIndex();
            }
            String categoryName = "";
            for (Category cat : categoryList) {
                if (cat.getCategoryId() == product.getCategoryId()) {
                    categoryName = cat.getCategoryName();
                    break;
                }
            }
            
            Object rowNumberToShow;
            if (isSubGroup) {
                rowNumberToShow = "";
            } else {
                int mainGroupSeq = 0;
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
                        mainGroupSeq++;
                    }
                }
                rowNumberToShow = mainGroupSeq + 1;
            }
            
            java.math.BigDecimal price = product.getPrice();
            java.math.BigDecimal total;
            int finalQuantity = quantity;
            if (isSubGroup && parentGroupIndex >= 0) {
                int parentQuantity = mainGroupQuantities.get(parentGroupIndex);
                finalQuantity = parentQuantity * quantity;
                if (cmValue != null) {
                    total = price.multiply(java.math.BigDecimal.valueOf(finalQuantity)).multiply(cmValue);
                } else {
                    total = price.multiply(java.math.BigDecimal.valueOf(finalQuantity));
                }
            } else {
                if (cmValue != null) {
                    total = price.multiply(java.math.BigDecimal.valueOf(quantity)).multiply(cmValue);
                } else {
                    total = price.multiply(java.math.BigDecimal.valueOf(quantity));
                }
            }
            
            java.math.BigDecimal laborCost = java.math.BigDecimal.ZERO;
            invoiceTableModel.addRow(new Object[]{
                rowNumberToShow,
                product.getProductName(),
                categoryName,
                price,
                cmValue,
                finalQuantity,
                laborCost,
                total
            });
            calculateTotals();
        }
    }
} 