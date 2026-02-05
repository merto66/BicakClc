package com.bicakclc.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.math.BigDecimal;
import java.util.ArrayList;
import javax.swing.table.TableRowSorter;

import com.bicakclc.model.Category;
import com.bicakclc.model.Product;
import com.bicakclc.service.CategoryService;
import com.bicakclc.service.FavoriteService;
import com.bicakclc.service.ProductService;

import javax.swing.table.TableModel;

public class ProductPanel extends JPanel {
    private FavoriteService favoriteService;
    private JButton favoriteButton;
    private JTable productTable;
    private ProductService productService;
    private CategoryService categoryService;
    private List<Category> categoryList;
    private FavoritePanel favoritePanel;
    private JComboBox<String> categoryComboBox;

    public ProductPanel(FavoritePanel favoritePanel) {
        this.favoritePanel = favoritePanel;
        setLayout(new BorderLayout());
        favoriteService = new FavoriteService();
        productService = new ProductService();
        categoryService = new CategoryService();
        categoryList = new ArrayList<>();
        initializeComponents();
        refreshProductTable(); // Automatically refresh on startup
    }

    private void initializeComponents() {
        try {
            categoryList = categoryService.getAllCategories();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Kategoriler yüklenemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
        // Create table
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Ad", "Kategori", "Fiyat", "Birim"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // Only allow editing except ID
            }
        };
        productTable = new JTable(model);
        // Enable sorting by clicking column headers
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        productTable.setRowSorter(sorter);
        // Ensure numeric sorting for Fiyat column (index 3)
        sorter.setComparator(3, (o1, o2) -> {
            if (o1 instanceof BigDecimal && o2 instanceof BigDecimal) {
                return ((BigDecimal) o1).compareTo((BigDecimal) o2);
            }
            try {
                return new BigDecimal(o1.toString()).compareTo(new BigDecimal(o2.toString()));
            } catch (Exception e) {
                return 0;
            }
        });
        // Ensure natural numeric sorting for Ad column (index 1)
        sorter.setComparator(1, (o1, o2) -> {
            // Extract leading number (before "MM" or space)
            double n1 = extractLeadingNumber(o1);
            double n2 = extractLeadingNumber(o2);
            return Double.compare(n1, n2);
        });
        // Set up category combo box editor
        categoryComboBox = new JComboBox<>();
        for (Category cat : categoryList) {
            categoryComboBox.addItem(cat.getCategoryName());
        }
        productTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(categoryComboBox));
        JScrollPane scrollPane = new JScrollPane(productTable);
        add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Yenile");
        JButton addButton = new JButton("Ekle");
        JButton deleteButton = new JButton("Sil");
        JButton saveButton = new JButton("Kaydet");
        JButton addFavoriteButton = new JButton("Favorilere Ekle");
        buttonPanel.add(refreshButton);
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(addFavoriteButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Refresh (Yenile)
        refreshButton.addActionListener(e -> refreshProductTable());
        // Add (Ekle)
        addButton.addActionListener(e -> {
            model.addRow(new Object[]{null, "", categoryList.isEmpty() ? "" : categoryList.get(0).getCategoryName(), BigDecimal.ZERO, ""});
        });
        // Delete (Sil)
        deleteButton.addActionListener(e -> {
            int selectedRow = productTable.getSelectedRow();
            if (selectedRow >= 0) {
                int confirm = JOptionPane.showConfirmDialog(this, "Seçili ürünü silmek istediğinize emin misiniz?", "Onay", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Object idObj = model.getValueAt(selectedRow, 0);
                    if (idObj != null) {
                        try {
                            int id = Integer.parseInt(idObj.toString());
                            productService.deleteProduct(id);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this, "Silme işlemi başarısız: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    model.removeRow(selectedRow);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Lütfen silmek için bir satır seçin.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            }
        });
        // Save (Kaydet)
        saveButton.addActionListener(e -> {
            if (productTable.isEditing()) {
                productTable.getCellEditor().stopCellEditing();
            }
            try {
                for (int i = 0; i < model.getRowCount(); i++) {
                    Object idObj = model.getValueAt(i, 0);
                    String name = (String) model.getValueAt(i, 1);
                    String categoryName = (String) model.getValueAt(i, 2);
                    BigDecimal price = null;
                    try {
                        price = new BigDecimal(model.getValueAt(i, 3).toString());
                    } catch (Exception ex) {
                        price = BigDecimal.ZERO;
                    }
                    String unit = (String) model.getValueAt(i, 4);
                    if (name == null || name.trim().isEmpty()) continue;
                    Product product = new Product();
                    product.setProductName(name);
                    product.setPrice(price);
                    product.setUnit(unit);
                    // Find categoryId by name
                    int categoryId = 1;
                    for (Category cat : categoryList) {
                        if (cat.getCategoryName().equals(categoryName)) {
                            categoryId = cat.getCategoryId();
                            break;
                        }
                    }
                    product.setCategoryId(categoryId);
                    product.setProductCode("");
                    product.setThickness(BigDecimal.ZERO);
                    product.setSizeMm(0);
                    product.setActive(true);
                    if (idObj == null) {
                        // New product
                        productService.addProduct(product);
                    } else {
                        // Existing product
                        product.setProductId(Integer.parseInt(idObj.toString()));
                        productService.updateProduct(product);
                    }
                }
                // Reload table
                model.setRowCount(0);
                List<Product> products = productService.getAllProducts();
                for (Product product : products) {
                    // Find category name by id
                    String catName = "";
                    for (Category cat : categoryList) {
                        if (cat.getCategoryId() == product.getCategoryId()) {
                            catName = cat.getCategoryName();
                            break;
                        }
                    }
                    model.addRow(new Object[]{
                        product.getProductId(),
                        product.getProductName(),
                        catName,
                        product.getPrice(),
                        product.getUnit()
                    });
                }
                productTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(categoryComboBox));
                JOptionPane.showMessageDialog(this, "Ürünler kaydedildi.", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Kaydetme işlemi başarısız: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
        });
        // Favorilere Ekle
        addFavoriteButton.addActionListener(e -> {
            int selectedRow = productTable.getSelectedRow();
            if (selectedRow >= 0) {
                Object idObj = model.getValueAt(selectedRow, 0);
                if (idObj != null) {
                    try {
                        int productId = Integer.parseInt(idObj.toString());
                        if (favoriteService.isFavorite(productId)) {
                            JOptionPane.showMessageDialog(this, "Bu ürün zaten favorilerde!", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                        favoriteService.addFavorite(productId);
                            com.bicakclc.ui.InvoicePanel.refreshAllFavoritePanels();
                        JOptionPane.showMessageDialog(this, "Ürün favorilere eklendi.", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Favorilere ekleme başarısız: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Lütfen favorilere eklemek için bir ürün seçin.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private void addFavoriteButton(Product product) {
        try {
            favoriteButton = new JButton(favoriteService.isFavorite(product.getProductId()) ? "★" : "☆");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error checking favorite status: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
        favoriteButton.addActionListener(e -> toggleFavorite(product, favoriteButton));
    }

    private void toggleFavorite(Product product, JButton button) {
        try {
            if (favoriteService.isFavorite(product.getProductId())) {
                favoriteService.removeFavorite(product.getProductId());
                button.setText("☆");
            } else {
                favoriteService.addFavorite(product.getProductId());
                button.setText("★");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error toggling favorite: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updateProducts(List<Product> products) {
        DefaultTableModel model = (DefaultTableModel) productTable.getModel();
        model.setRowCount(0);
        for (Product product : products) {
            // Find category name by id
            String catName = "";
            for (Category cat : categoryList) {
                if (cat.getCategoryId() == product.getCategoryId()) {
                    catName = cat.getCategoryName();
                    break;
                }
            }
            model.addRow(new Object[]{
                product.getProductId(),
                product.getProductName(),
                catName,
                product.getPrice(),
                product.getUnit()
            });
        }
        // Re-apply the cell editor for the category column
        productTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(categoryComboBox));
    }

    // Helper function
    private static double extractLeadingNumber(Object o) {
        if (o == null) return 0;
        String s = o.toString().trim();
        try {
            // Find first number in the string
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^[0-9]+(\\.[0-9]+)?").matcher(s);
            if (m.find()) {
                return Double.parseDouble(m.group());
            }
        } catch (Exception e) {}
        return 0;
    }

    // Refresh product table from database
    private void refreshProductTable() {
        try {
            List<Product> products = productService.getAllProducts();
            updateProducts(products);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ürünler yenilenemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
}
