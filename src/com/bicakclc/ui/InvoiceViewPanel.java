package com.bicakclc.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.sql.Connection;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JLabel;
import java.awt.Font;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.bicakclc.model.Category;
import com.bicakclc.model.Invoice;
import com.bicakclc.model.InvoiceItem;
import com.bicakclc.model.Product;
import com.bicakclc.service.CategoryService;
import com.bicakclc.service.InvoiceItemService;
import com.bicakclc.service.InvoiceService;
import com.bicakclc.service.ProductService;
import com.bicakclc.util.DatabaseConnection;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.SpreadsheetVersion;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;

public class InvoiceViewPanel extends JPanel {
    private InvoiceService invoiceService;
    private InvoiceItemService invoiceItemService;
    private ProductService productService;
    private CategoryService categoryService;
    private Connection connection;
    
    // UI Components
    private JTable invoiceTable;
    private DefaultTableModel invoiceTableModel;
    private JButton refreshButton;
    private JButton deleteButton;
    private JButton viewDetailsButton;
    private JButton exportExcelButton;
    private JTextField companyNameFilterField;
    private JTextField qualityFilterField;
    private JButton clearFiltersButton;
    
    // Data
    private List<Invoice> invoiceList;
    private List<Product> productList;
    private List<Category> categoryList;
    
    public InvoiceViewPanel() {
        setLayout(new BorderLayout());
        initializeServices();
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
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Servis başlatılamadı: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void initializeComponents() {
        // Top panel with buttons and filters
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // Buttons panel
        JPanel buttonsPanel = createTopPanel();
        topPanel.add(buttonsPanel, BorderLayout.NORTH);
        
        // Filter panel
        JPanel filterPanel = createFilterPanel();
        topPanel.add(filterPanel, BorderLayout.CENTER);
        
        // Center panel - Invoice table
        createInvoiceTable();
        JScrollPane scrollPane = new JScrollPane(invoiceTable);
        
        // Layout
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Fatura İşlemleri"));
        
        refreshButton = new JButton("Yenile");
        viewDetailsButton = new JButton("Detayları Görüntüle");
        deleteButton = new JButton("Faturayı Sil");
        exportExcelButton = new JButton("Excel'e Aktar");
        
        refreshButton.addActionListener(e -> loadData());
        viewDetailsButton.addActionListener(e -> viewInvoiceDetails());
        deleteButton.addActionListener(e -> deleteInvoice());
        exportExcelButton.addActionListener(e -> exportTableToExcel());
        
        panel.add(refreshButton);
        panel.add(viewDetailsButton);
        panel.add(deleteButton);
        panel.add(exportExcelButton);
        return panel;
    }
    
    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Filtreleme"));
        
        // Company name filter
        JLabel companyLabel = new JLabel("Firma Adı:");
        companyNameFilterField = new JTextField(15);
        companyNameFilterField.setToolTipText("Firma adına göre filtrele");
        
        // Quality filter
        JLabel qualityLabel = new JLabel("Kalite:");
        qualityFilterField = new JTextField(15);
        qualityFilterField.setToolTipText("Kaliteye göre filtrele");
        
        // Clear filters button
        clearFiltersButton = new JButton("Filtreleri Temizle");
        clearFiltersButton.addActionListener(e -> clearFilters());
        
        // Add listeners for real-time filtering
        companyNameFilterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
        });
        
        qualityFilterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
        });
        
        panel.add(companyLabel);
        panel.add(companyNameFilterField);
        panel.add(qualityLabel);
        panel.add(qualityFilterField);
        panel.add(clearFiltersButton);
        
        return panel;
    }
    
    private void filterTable() {
        String companyFilter = companyNameFilterField.getText().toLowerCase().trim();
        String qualityFilter = qualityFilterField.getText().toLowerCase().trim();
        
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) invoiceTable.getRowSorter();
        if (sorter == null) {
            sorter = new TableRowSorter<>(invoiceTableModel);
            invoiceTable.setRowSorter(sorter);
        }
        
        // Clear existing filters
        sorter.setRowFilter(null);
        
        // Create combined filter for both company name and quality
        if (!companyFilter.isEmpty() || !qualityFilter.isEmpty()) {
            sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    String companyName = entry.getStringValue(1).toLowerCase();
                    String quality = entry.getStringValue(2).toLowerCase();
                    
                    boolean companyMatch = companyFilter.isEmpty() || companyName.contains(companyFilter);
                    boolean qualityMatch = qualityFilter.isEmpty() || quality.contains(qualityFilter);
                    
                    return companyMatch && qualityMatch;
                }
            });
        }
    }
    
    private void clearFilters() {
        companyNameFilterField.setText("");
        qualityFilterField.setText("");
        filterTable();
    }
    
    private void createInvoiceTable() {
        String[] columnNames = {"Fatura No", "Firma Adı", "Kalite", "Tarih", "Toplam Tutar", "İskonto", "Son Tutar", "Durum"};
        invoiceTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only table
            }
        };
        
        invoiceTable = new JTable(invoiceTableModel);
        invoiceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add double-click listener for viewing details
        invoiceTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewInvoiceDetails();
                }
            }
        });
        
        // Add table sorter
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(invoiceTableModel);
        invoiceTable.setRowSorter(sorter);
    }
    
    private void loadData() {
        try {
            invoiceList = invoiceService.getAllInvoices();
            productList = productService.getAllProducts();
            categoryList = categoryService.getAllCategories();
            
            refreshTable();
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Veri yüklenemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refreshTable() {
        invoiceTableModel.setRowCount(0);
        
        for (Invoice invoice : invoiceList) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            String formattedDate = invoice.getInvoiceDate().format(formatter);
            
            invoiceTableModel.addRow(new Object[]{
                invoice.getInvoiceNumber(),
                invoice.getCompanyName(),
                invoice.getQuality(),
                formattedDate,
                String.format("%.2f TL", invoice.getTotalAmount()),
                String.format("%.2f TL", invoice.getDiscountAmount()),
                String.format("%.2f TL", invoice.getFinalAmount()),
                invoice.getStatus()
            });
        }
    }
    
    private void viewInvoiceDetails() {
        int selectedRow = invoiceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Lütfen görüntülemek için bir fatura seçin.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Convert to model index if table is sorted
        int modelRow = invoiceTable.convertRowIndexToModel(selectedRow);
        String invoiceNumber = invoiceTableModel.getValueAt(modelRow, 0).toString();
        
        try {
            // Find invoice
            Invoice selectedInvoice = null;
            for (Invoice invoice : invoiceList) {
                if (invoice.getInvoiceNumber().equals(invoiceNumber)) {
                    selectedInvoice = invoice;
                    break;
                }
            }
            
            if (selectedInvoice != null) {
                showInvoiceDetailsDialog(selectedInvoice);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Fatura detayları yüklenemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showInvoiceDetailsDialog(Invoice invoice) {
        try {
            List<InvoiceItem> items = invoiceItemService.getInvoiceItemsByInvoiceId(invoice.getInvoiceId());
            
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Fatura Detayları", true);
            dialog.setLayout(new BorderLayout());
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(this);
            
            // Invoice info panel
            JPanel infoPanel = new JPanel(new GridBagLayout());
            infoPanel.setBorder(BorderFactory.createTitledBorder("Fatura Bilgileri"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            
            gbc.gridx = 0; gbc.gridy = 0;
            infoPanel.add(new JLabel("Fatura No:"), gbc);
            gbc.gridx = 1;
            infoPanel.add(new JLabel(invoice.getInvoiceNumber()), gbc);
            
            gbc.gridx = 0; gbc.gridy = 1;
            infoPanel.add(new JLabel("Firma Adı:"), gbc);
            gbc.gridx = 1;
            infoPanel.add(new JLabel(invoice.getCompanyName()), gbc);
            
            gbc.gridx = 0; gbc.gridy = 2;
            infoPanel.add(new JLabel("Kalite:"), gbc);
            gbc.gridx = 1;
            infoPanel.add(new JLabel(invoice.getQuality()), gbc);
            
            gbc.gridx = 0; gbc.gridy = 3;
            infoPanel.add(new JLabel("Tarih:"), gbc);
            gbc.gridx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            infoPanel.add(new JLabel(invoice.getInvoiceDate().format(formatter)), gbc);
            
            // Items table
            String[] columnNames = {"Sıra", "Ürün Adı", "Fiyat", "CM", "Adet", "Toplam"};
            DefaultTableModel itemsModel = new DefaultTableModel(columnNames, 0);
            
            // Ana gruplar için sıra numarasını yeniden hesapla
            int mainGroupSeq = 0;
            for (InvoiceItem item : items) {
                // Find product name
                String productName = "";
                for (Product product : productList) {
                    if (product.getProductId() == item.getProductId()) {
                        productName = product.getProductName();
                        break;
                    }
                }
                
                // Alt gruplar için sıra numarasını boş göster, ana gruplar için sıralı numara ver
                Object rowNumberToShow;
                if (item.isSubGroup()) {
                    rowNumberToShow = "";
                } else {
                    mainGroupSeq++;
                    rowNumberToShow = mainGroupSeq;
                }
                
                itemsModel.addRow(new Object[]{
                    rowNumberToShow,
                    productName,
                    String.format("%.2f TL", item.getPrice()),
                    item.getCmValue() != null ? item.getCmValue().toString() : "",
                    item.getQuantity(),
                    String.format("%.2f TL", item.getTotal())
                });
            }
            
            JTable itemsTable = new JTable(itemsModel);
            JScrollPane itemsScrollPane = new JScrollPane(itemsTable);
            
            // Totals panel
            JPanel totalsPanel = new JPanel(new GridBagLayout());
            totalsPanel.setBorder(BorderFactory.createTitledBorder("Tutar Bilgileri"));
            GridBagConstraints gbc2 = new GridBagConstraints();
            gbc2.insets = new Insets(5, 5, 5, 5);
            
            gbc2.gridx = 0; gbc2.gridy = 0;
            totalsPanel.add(new JLabel("Toplam Tutar:"), gbc2);
            gbc2.gridx = 1;
            totalsPanel.add(new JLabel(String.format("%.2f TL", invoice.getTotalAmount())), gbc2);
            
            gbc2.gridx = 0; gbc2.gridy = 1;
            totalsPanel.add(new JLabel("İskonto:"), gbc2);
            gbc2.gridx = 1;
            totalsPanel.add(new JLabel(String.format("%.2f TL", invoice.getDiscountAmount())), gbc2);
            
            gbc2.gridx = 0; gbc2.gridy = 2;
            totalsPanel.add(new JLabel("Son Tutar:"), gbc2);
            gbc2.gridx = 1;
            double finalAmount = invoice.getFinalAmount() != null ? invoice.getFinalAmount().doubleValue() : 0.0;
            JLabel detailsFinalAmountLabel = new JLabel(String.format("%.2f TL", finalAmount));
            detailsFinalAmountLabel.setFont(new Font("Arial", Font.BOLD, 14));
            totalsPanel.add(detailsFinalAmountLabel, gbc2);

            // Toplam Adet
            gbc2.gridx = 0; gbc2.gridy = 3;
            totalsPanel.add(new JLabel("Toplam Adet:"), gbc2);
            gbc2.gridx = 1;
            totalsPanel.add(new JLabel(String.valueOf(invoice.getTotalQuantity() != null ? invoice.getTotalQuantity() : 0)), gbc2);
            
            // Close and Export buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton exportButton = new JButton("Excel'e Aktar");
            JButton closeButton = new JButton("Kapat");
            closeButton.addActionListener(e -> dialog.dispose());
            exportButton.addActionListener(e -> exportSelectedInvoiceToExcel(invoice, items));
            buttonPanel.add(exportButton);
            buttonPanel.add(closeButton);

            // Layout
            JPanel centerPanel = new JPanel(new BorderLayout());
            centerPanel.add(infoPanel, BorderLayout.NORTH);
            centerPanel.add(itemsScrollPane, BorderLayout.CENTER);
            centerPanel.add(totalsPanel, BorderLayout.SOUTH);

            dialog.add(centerPanel, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setVisible(true);
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Fatura detayları yüklenemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteInvoice() {
        int selectedRow = invoiceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Lütfen silmek için bir fatura seçin.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Bu faturayı silmek istediğinize emin misiniz?\nBu işlem geri alınamaz.", 
            "Onay", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Convert to model index if table is sorted
                int modelRow = invoiceTable.convertRowIndexToModel(selectedRow);
                String invoiceNumber = invoiceTableModel.getValueAt(modelRow, 0).toString();
                
                // Find and delete invoice
                for (Invoice invoice : invoiceList) {
                    if (invoice.getInvoiceNumber().equals(invoiceNumber)) {
                        invoiceService.deleteInvoice(invoice.getInvoiceId());
                        break;
                    }
                }
                
                loadData(); // Refresh table
                JOptionPane.showMessageDialog(this, "Fatura başarıyla silindi.", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Fatura silinemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportTableToExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Excel dosyası kaydet");
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".xlsx");
            }
            try (XSSFWorkbook workbook = new XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(fileToSave)) {
                Sheet sheet = workbook.createSheet("Faturalar");
                // Header
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < invoiceTable.getColumnCount(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(invoiceTable.getColumnName(i));
                }
                // Data
                for (int row = 0; row < invoiceTable.getRowCount(); row++) {
                    Row excelRow = sheet.createRow(row + 1);
                    for (int col = 0; col < invoiceTable.getColumnCount(); col++) {
                        Object value = invoiceTable.getValueAt(row, col);
                        Cell cell = excelRow.createCell(col);
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        } else {
                            cell.setCellValue("");
                        }
                    }
                }
                // Otomatik sütun genişliği
                for (int i = 0; i < invoiceTable.getColumnCount(); i++) {
                    sheet.autoSizeColumn(i);
                }
                workbook.write(fos);
                JOptionPane.showMessageDialog(this, "Excel dosyası başarıyla kaydedildi.", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Excel kaydedilirken hata: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportSelectedInvoiceToExcel(Invoice invoice, java.util.List<InvoiceItem> items) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Faturayı Excel'e Kaydet");
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new java.io.File(fileToSave.getAbsolutePath() + ".xlsx");
            }
            try (InputStream templateStream = openInvoiceTemplateStream();
                 Workbook workbook = WorkbookFactory.create(templateStream);
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(fileToSave)) {

                // Template'in ilk sheet'ini kullan (named range'ler zaten doğru sheet'i işaret eder)
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
                // Stiller
                org.apache.poi.ss.usermodel.CellStyle boldStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
                boldFont.setBold(true);
                boldFont.setFontHeightInPoints((short)11);
                boldStyle.setFont(boldFont);

                org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setFontHeightInPoints((short)12);
                headerStyle.setFont(headerFont);
                headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
                headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                headerStyle.setFillForegroundColor((short) 22); // light blue
                headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

                org.apache.poi.ss.usermodel.CellStyle moneyStyle = workbook.createCellStyle();
                moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00 TL"));
                moneyStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT);
                moneyStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                moneyStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                moneyStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                moneyStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

                org.apache.poi.ss.usermodel.CellStyle normalStyle = workbook.createCellStyle();
                normalStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                normalStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                normalStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                normalStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

                // ===== Named range ile üst bilgiler =====
                // Gerekli named range'ler:
                // - InvoiceNumberCell, CompanyNameCell, QualityCell, DateCell
                // - ItemsStartCell (tablonun ilk satırının ilk hücresi, örn A12)
                // - TotalAmountCell, DiscountCell, FinalAmountCell, TotalQtyCell

                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

                // Named range'ler yoksa kullanıcıya eksikleri gösterip çık
                java.util.List<String> missingNames = new java.util.ArrayList<>();
                if (!setNamedCellString(workbook, "InvoiceNumberCell", invoice.getInvoiceNumber())) missingNames.add("InvoiceNumberCell");
                if (!setNamedCellString(workbook, "CompanyNameCell", invoice.getCompanyName())) missingNames.add("CompanyNameCell");
                if (!setNamedCellString(workbook, "QualityCell", invoice.getQuality())) missingNames.add("QualityCell");
                if (!setNamedCellString(workbook, "DateCell", invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(dtf) : "")) missingNames.add("DateCell");

                // Üst bilgi alanları eksikse daha ileri gitme (NPE ve yanlış dosya üretimini engelle)
                if (!missingNames.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Template içinde gerekli named range'ler bulunamadı:\n\n");
                    for (String n : missingNames) sb.append("- ").append(n).append("\n");
                    sb.append("\nExcel'de hücreyi seçip sol üstteki Name Box'a (formül çubuğunun solundaki kutu) bu adı yazıp Enter'a basarak ekleyebilirsin.\n");
                    sb.append("\nTemplate'te mevcut named range'ler:\n");
                    sb.append(listWorkbookNames(workbook));
                    JOptionPane.showMessageDialog(this, sb.toString(), "Template Named Range Eksik", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Adım 1: Her satırın toplamını hesapla (önce DB'den gelen total'a güven, yoksa price*adet*(cm) hesapla)
                java.util.Map<Integer, java.math.BigDecimal> itemCalculatedTotals = new java.util.HashMap<>();
                for (InvoiceItem item : items) {
                    if (item.getItemId() == null) {
                        continue;
                    }

                    java.math.BigDecimal calculatedTotal = java.math.BigDecimal.ZERO;
                    if (item.getTotal() != null) {
                        calculatedTotal = item.getTotal();
                    } else if (item.getPrice() != null && item.getQuantity() != null) {
                        calculatedTotal = item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()));
                        if (item.getCmValue() != null) {
                            calculatedTotal = calculatedTotal.multiply(item.getCmValue());
                        }
                    }

                    itemCalculatedTotals.put(item.getItemId(), calculatedTotal);
                }

                // Adım 2: Excel'deki sıra mantığıyla grup toplamını hesapla
                // Kural: Bir ana grup satırından sonra gelen alt satırlar, bir sonraki ana gruba kadar o gruba aittir.
                java.util.Map<Integer, java.math.BigDecimal> mainGroupTotals = new java.util.HashMap<>();
                Integer currentMainItemId = null;
                java.math.BigDecimal runningGroupTotal = java.math.BigDecimal.ZERO;

                for (InvoiceItem item : items) {
                    if (item.getItemId() == null) {
                        continue;
                    }

                    java.math.BigDecimal rowTotal = itemCalculatedTotals.getOrDefault(item.getItemId(), java.math.BigDecimal.ZERO);

                    if (!item.isSubGroup()) {
                        // Yeni ana grup başladı: önce önceki grubun toplamını kaydet
                        if (currentMainItemId != null) {
                            mainGroupTotals.put(currentMainItemId, runningGroupTotal);
                        }
                        currentMainItemId = item.getItemId();
                        runningGroupTotal = rowTotal;
                    } else {
                        // Alt grup: en son görülen ana gruba ekle
                        if (currentMainItemId != null) {
                            runningGroupTotal = runningGroupTotal.add(rowTotal);
                        }
                    }
                }

                // Son grubu kaydet
                if (currentMainItemId != null) {
                    mainGroupTotals.put(currentMainItemId, runningGroupTotal);
                }
                
                // ===== Named range ile tablo başlangıcı =====
                Cell itemsStart = getNamedCell(workbook, "ItemsStartCell");
                if (itemsStart == null) {
                    missingNames.add("ItemsStartCell");
                    StringBuilder sb = new StringBuilder();
                    sb.append("Template içinde gerekli named range'ler bulunamadı:\n\n");
                    for (String n : missingNames) sb.append("- ").append(n).append("\n");
                    sb.append("\nExcel'de hücreyi seçip sol üstteki Name Box'a (formül çubuğunun solundaki kutu) bu adı yazıp Enter'a basarak ekleyebilirsin.\n");
                    sb.append("\nTemplate'te mevcut named range'ler:\n");
                    sb.append(listWorkbookNames(workbook));
                    JOptionPane.showMessageDialog(this, sb.toString(), "Template Named Range Eksik", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int startRow = itemsStart.getRowIndex();
                int startCol = itemsStart.getColumnIndex();

                // Header satırını (ItemsStartCell'in bir üst satırı) export sırasında garanti altına al
                // Kolonlar: Sıra, Ürün Adı, CM, Adet, Birim Fiyat, Toplam
                // NOT: Template'te header zaten varsa EZMEYELİM. Sadece boşsa yaz.
                int headerRowIdx = Math.max(0, startRow - 1);
                Row headerRow = sheet.getRow(headerRowIdx);
                if (headerRow == null) headerRow = sheet.createRow(headerRowIdx);
                String[] exportHeaders = {"Sıra", "Ürün Adı", "CM", "Adet", "Birim Fiyat", "Toplam"};
                boolean headerLooksEmpty = true;
                for (int i = 0; i < exportHeaders.length; i++) {
                    Cell hc = headerRow.getCell(startCol + i);
                    if (hc != null && !isCellBlank(hc)) {
                        headerLooksEmpty = false;
                        break;
                    }
                }
                if (headerLooksEmpty) {
                    for (int i = 0; i < exportHeaders.length; i++) {
                        Cell hc = getOrCreateCell(headerRow, startCol + i);
                        hc.setCellValue(exportHeaders[i]);
                        hc.setCellStyle(headerStyle);
                    }
                }

                // Footer hücrelerinin en üst satırını bul (satırları gerekirse aşağı kaydıracağız)
                int footerTopRow = Integer.MAX_VALUE;
                footerTopRow = Math.min(footerTopRow, getNamedRowIndex(workbook, "TotalAmountCell"));
                footerTopRow = Math.min(footerTopRow, getNamedRowIndex(workbook, "DiscountCell"));
                footerTopRow = Math.min(footerTopRow, getNamedRowIndex(workbook, "FinalAmountCell"));
                footerTopRow = Math.min(footerTopRow, getNamedRowIndex(workbook, "TotalQtyCell"));
                if (footerTopRow == Integer.MAX_VALUE) {
                    // Footer named range yoksa, satır kaydırma yapma (template sabit satır sayısı varsayılır)
                    footerTopRow = startRow + 9999;
                }

                // Template'te footer yukarıdaysa ve satır sayısı taşacaksa, footer'ı aşağı kaydır
                int requiredEndRowExclusive = startRow + Math.max(items.size(), 1);
                if (requiredEndRowExclusive >= footerTopRow) {
                    int delta = (requiredEndRowExclusive - footerTopRow) + 1;
                    sheet.shiftRows(footerTopRow, sheet.getLastRowNum(), delta, true, false);
                    // shiftRows named range referanslarını otomatik güncellemeyebilir; footer hücrelerini de kaydır
                    shiftNamedCellRowIfNeeded(workbook, "TotalAmountCell", footerTopRow, delta);
                    shiftNamedCellRowIfNeeded(workbook, "DiscountCell", footerTopRow, delta);
                    shiftNamedCellRowIfNeeded(workbook, "FinalAmountCell", footerTopRow, delta);
                    shiftNamedCellRowIfNeeded(workbook, "TotalQtyCell", footerTopRow, delta);
                }

                // Fatura kalemleri - Tüm öğeleri göster (ana grup + alt gruplar)
                int mainGroupSeq = 1;
                for (int idx = 0; idx < items.size(); idx++) {
                    InvoiceItem item = items.get(idx);
                    // Ürün adı boşsa productList'ten bul
                    if (item.getProductName() == null || item.getProductName().isEmpty()) {
                        for (Product p : productList) {
                            if (p.getProductId() == item.getProductId()) {
                                item.setProductName(p.getProductName());
                                break;
                            }
                        }
                    }

                    int excelRowIdx = startRow + idx;
                    org.apache.poi.ss.usermodel.Row itemRow = sheet.getRow(excelRowIdx);
                    if (itemRow == null) {
                        itemRow = sheet.createRow(excelRowIdx);
                    }

                    // Sıra numarası: Ana grup için sıra numarası, alt grup için boş
                    if (!item.isSubGroup()) {
                        Cell c = getOrCreateCell(itemRow, startCol + 0);
                        c.setCellValue(mainGroupSeq++);
                        c.setCellStyle(normalStyle);
                    } else {
                        Cell c = getOrCreateCell(itemRow, startCol + 0);
                        c.setCellValue("");
                        c.setCellStyle(normalStyle);
                    }

                    // Ürün Adı
                    org.apache.poi.ss.usermodel.Cell pnameCell = getOrCreateCell(itemRow, startCol + 1);
                    pnameCell.setCellValue(item.getProductName() != null ? item.getProductName() : "");
                    pnameCell.setCellStyle(normalStyle);

                    // CM: Her ikisinde de göster
                    org.apache.poi.ss.usermodel.Cell cmCell = getOrCreateCell(itemRow, startCol + 2);
                    cmCell.setCellValue(item.getCmValue() != null ? item.getCmValue().doubleValue() : 0d);
                    cmCell.setCellStyle(normalStyle);

                    // Adet: Her ikisinde de göster
                    org.apache.poi.ss.usermodel.Cell qtyCell = getOrCreateCell(itemRow, startCol + 3);
                    qtyCell.setCellValue(item.getQuantity() != null ? item.getQuantity() : 0);
                    qtyCell.setCellStyle(normalStyle);

                    // Birim Fiyat: Sadece ana grup satırında yaz (Toplam / Adet)
                    org.apache.poi.ss.usermodel.Cell unitPriceCell = getOrCreateCell(itemRow, startCol + 4);
                    if (!item.isSubGroup()) {
                        int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                        java.math.BigDecimal rowTotal = mainGroupTotals.get(item.getItemId());
                        if (rowTotal == null) {
                            rowTotal = itemCalculatedTotals.getOrDefault(item.getItemId(), java.math.BigDecimal.ZERO);
                        }
                        if (qty > 0) {
                            // 2 ondalık, HALF_UP
                            java.math.BigDecimal unitPrice = rowTotal
                                    .divide(java.math.BigDecimal.valueOf(qty), 2, java.math.RoundingMode.HALF_UP);
                            unitPriceCell.setCellValue(unitPrice.doubleValue());
                            unitPriceCell.setCellStyle(moneyStyle);
                        } else {
                            unitPriceCell.setCellValue("");
                            unitPriceCell.setCellStyle(normalStyle);
                        }
                    } else {
                        unitPriceCell.setCellValue("");
                        unitPriceCell.setCellStyle(normalStyle);
                    }

                    // Toplam: Ana grup için sıranın toplam fiyatı (ana grup + alt gruplar), alt grup için boş
                    org.apache.poi.ss.usermodel.Cell totalCell = getOrCreateCell(itemRow, startCol + 5);
                    if (!item.isSubGroup()) {
                        java.math.BigDecimal rowTotal = mainGroupTotals.get(item.getItemId());
                        if (rowTotal != null) {
                            totalCell.setCellValue(rowTotal.doubleValue());
                        } else {
                            // Fallback: Eğer mainGroupTotals'da yoksa, sadece kendi hesaplanmış toplamını kullan
                            java.math.BigDecimal calculatedTotal = itemCalculatedTotals.getOrDefault(item.getItemId(), java.math.BigDecimal.ZERO);
                            totalCell.setCellValue(calculatedTotal.doubleValue());
                        }
                        totalCell.setCellStyle(moneyStyle);
                    } else {
                        totalCell.setCellValue("");
                        totalCell.setCellStyle(normalStyle);
                    }
                }
                // ===== Named range ile alt toplamlar =====
                if (!setNamedCellNumber(workbook, "TotalAmountCell", invoice.getTotalAmount() != null ? invoice.getTotalAmount().doubleValue() : 0d)) missingNames.add("TotalAmountCell");
                if (!setNamedCellNumber(workbook, "DiscountCell", invoice.getDiscountAmount() != null ? invoice.getDiscountAmount().doubleValue() : 0d)) missingNames.add("DiscountCell");
                if (!setNamedCellNumber(workbook, "FinalAmountCell", invoice.getFinalAmount() != null ? invoice.getFinalAmount().doubleValue() : 0d)) missingNames.add("FinalAmountCell");
                if (!setNamedCellNumber(workbook, "TotalQtyCell", invoice.getTotalQuantity() != null ? invoice.getTotalQuantity() : 0)) missingNames.add("TotalQtyCell");

                if (!missingNames.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Template içinde gerekli named range'ler bulunamadı:\n\n");
                    for (String n : missingNames) sb.append("- ").append(n).append("\n");
                    sb.append("\nExcel'de hücreyi seçip sol üstteki Name Box'a (formül çubuğunun solundaki kutu) bu adı yazıp Enter'a basarak ekleyebilirsin.\n");
                    sb.append("\nTemplate'te mevcut named range'ler:\n");
                    sb.append(listWorkbookNames(workbook));
                    JOptionPane.showMessageDialog(this, sb.toString(), "Template Named Range Eksik", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Otomatik sütun genişliği
                // Template zaten genişlik ayarlı olabilir; yine de tablo kolonlarını autosize yapalım
                for (int i = 0; i <= 5; i++) {
                    sheet.autoSizeColumn(startCol + i);
                }
                workbook.write(fos);
                JOptionPane.showMessageDialog(this, "Fatura başarıyla kaydedildi:\n" + fileToSave.getAbsolutePath());
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Excel'e aktarırken hata oluştu:\n" + ex.getMessage());
            }
        }
    }

    private InputStream openInvoiceTemplateStream() throws Exception {
        // 1) Classpath (önerilen): /templates/inovice_template.xlsx
        String[] classpathCandidates = new String[] {
                "/templates/inovice_template.xlsx",
                "/templates/invoice_template.xlsx",
                "/templates/inovice_temple.xlsx"
        };
        for (String cp : classpathCandidates) {
            InputStream in = InvoiceViewPanel.class.getResourceAsStream(cp);
            if (in != null) return in;
        }

        // 2) Proje içi dosya (IDE'de kolay)
        String[] diskCandidates = new String[] {
                "resources/templates/inovice_template.xlsx",
                "resources/templates/invoice_template.xlsx",
                "resources/templates/inovice_temple.xlsx",
                "src/main/resources/templates/inovice_template.xlsx",
                "src/main/resources/templates/invoice_template.xlsx",
                "src/main/resources/templates/inovice_temple.xlsx"
        };
        for (String p : diskCandidates) {
            File f = new File(p);
            if (f.exists()) return new FileInputStream(f);
        }

        throw new FileNotFoundException("Template bulunamadı. /templates/... (classpath) veya resources/templates/ ya da src/main/resources/templates/ altında bekleniyor.");
    }

    private static Cell getNamedCell(Workbook wb, String name) {
        // Excel'de isimler bazen sheet-scoped olabilir ve büyük/küçük harf farkıyla kayıtlı olabilir.
        Name nm = null;
        for (Name n : wb.getAllNames()) {
            if (n != null && n.getNameName() != null && n.getNameName().equalsIgnoreCase(name)) {
                nm = n;
                break;
            }
        }
        if (nm == null) return null;

        String refersTo = nm.getRefersToFormula();
        if (refersTo == null || refersTo.isBlank()) return null;

        AreaReference ar = new AreaReference(refersTo, SpreadsheetVersion.EXCEL2007);
        CellReference cr = ar.getFirstCell();

        Sheet sh = null;
        if (cr.getSheetName() != null) {
            sh = wb.getSheet(cr.getSheetName());
        }
        // Sheet adı referansta yoksa (sheet-scoped name), Name içindeki sheetIndex'i kullan
        if (sh == null && nm.getSheetIndex() >= 0 && nm.getSheetIndex() < wb.getNumberOfSheets()) {
            sh = wb.getSheetAt(nm.getSheetIndex());
        }
        if (sh == null) return null;

        Row r = sh.getRow(cr.getRow());
        if (r == null) r = sh.createRow(cr.getRow());
        Cell c = r.getCell(cr.getCol());
        if (c == null) c = r.createCell(cr.getCol());
        return c;
    }

    private static int getNamedRowIndex(Workbook wb, String name) {
        Cell c = getNamedCell(wb, name);
        return c != null ? c.getRowIndex() : Integer.MAX_VALUE;
    }

    private static boolean setNamedCellString(Workbook wb, String name, String value) {
        Cell c = getNamedCell(wb, name);
        if (c == null) return false;
        c.setCellValue(value != null ? value : "");
        return true;
    }

    private static boolean setNamedCellNumber(Workbook wb, String name, double value) {
        Cell c = getNamedCell(wb, name);
        if (c == null) return false;
        c.setCellValue(value);
        return true;
    }

    private static String listWorkbookNames(Workbook wb) {
        StringBuilder sb = new StringBuilder();
        for (Name n : wb.getAllNames()) {
            if (n == null) continue;
            String nm = n.getNameName();
            if (nm == null) continue;
            sb.append("- ").append(nm);
            if (n.getSheetIndex() >= 0 && n.getSheetIndex() < wb.getNumberOfSheets()) {
                sb.append(" (sheet: ").append(wb.getSheetName(n.getSheetIndex())).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static boolean isCellBlank(Cell cell) {
        if (cell == null) return true;
        return switch (cell.getCellType()) {
            case BLANK -> true;
            case STRING -> cell.getStringCellValue() == null || cell.getStringCellValue().trim().isEmpty();
            default -> false;
        };
    }

    private static void shiftNamedCellRowIfNeeded(Workbook wb, String name, int fromRowInclusive, int delta) {
        Name nm = null;
        for (Name n : wb.getAllNames()) {
            if (n != null && n.getNameName() != null && n.getNameName().equalsIgnoreCase(name)) {
                nm = n;
                break;
            }
        }
        if (nm == null) return;

        String refersTo = nm.getRefersToFormula();
        if (refersTo == null || refersTo.isBlank()) return;

        AreaReference ar = new AreaReference(refersTo, SpreadsheetVersion.EXCEL2007);
        CellReference cr = ar.getFirstCell();
        int row = cr.getRow();
        int col = cr.getCol();

        if (row < fromRowInclusive) return;

        String sheetName = cr.getSheetName();
        if (sheetName == null && nm.getSheetIndex() >= 0 && nm.getSheetIndex() < wb.getNumberOfSheets()) {
            sheetName = wb.getSheetName(nm.getSheetIndex());
        }

        CellReference shifted = new CellReference(sheetName, row + delta, col, true, true);
        nm.setRefersToFormula(shifted.formatAsString());
    }

    private static Cell getOrCreateCell(Row row, int colIdx) {
        Cell c = row.getCell(colIdx);
        if (c == null) c = row.createCell(colIdx);
        return c;
    }
} 