// FavoritePanel.java
package com.bicakclc.ui;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

import javax.swing.table.DefaultTableModel;

import com.bicakclc.model.Product;
import com.bicakclc.service.FavoriteService;

import java.util.List;

public class FavoritePanel extends JPanel {
    private JTable favoriteTable;
    private DefaultTableModel tableModel;
    private FavoriteService favoriteService;
    private JButton removeButton;
    
    // Add this interface for callback
    public interface AddToInvoiceListener {
        void addProductToInvoice(com.bicakclc.model.Product product);
    }
    private AddToInvoiceListener addToInvoiceListener;
    public void setAddToInvoiceListener(AddToInvoiceListener listener) {
        this.addToInvoiceListener = listener;
    }
    
    public FavoritePanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(400, 300));
        favoriteService = new FavoriteService();
        initializeComponents();
        // Register this panel for global refresh
        com.bicakclc.ui.InvoicePanel.registerFavoritePanel(this);
    }
    
    private void initializeComponents() {
        // Create table model
        String[] columns = {"Ürün", "Kategori", "Fiyat", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // No editable columns
            }
        };
        
        // Create table
        favoriteTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(favoriteTable);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        removeButton = new JButton("Favorilerden Kaldır");
        removeButton.addActionListener(e -> removeSelectedFavorite());
        buttonPanel.add(removeButton);
        
        // Add components
        add(new JLabel("Favoriler"), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Load favorites
        loadFavorites();
        // Add mouse listener for double-click
        favoriteTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = favoriteTable.getSelectedRow();
                    if (row >= 0) {
                        Product product = getProductAtRow(row);
                        if (addToInvoiceListener != null && product != null) {
                            addToInvoiceListener.addProductToInvoice(product);
                        }
                    }
                }
            }
        });
        // Add 'Faturaya Ekle' button to each row (rendered as a button in the Actions column)
        favoriteTable.getColumn("Actions").setCellRenderer(new ButtonRenderer());
        favoriteTable.getColumn("Actions").setCellEditor(new ButtonEditor(new JCheckBox(), this));
    }
    
    private void loadFavorites() {
        try {
            tableModel.setRowCount(0);
            List<Product> favorites = favoriteService.getFavoriteProducts();
            for (Product product : favorites) {
                String categoryName = product.getCategoryName();
                tableModel.addRow(new Object[]{
                    product.getProductName(),
                    categoryName,
                    product.getPrice(),
                    "Kaldır"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Favoriler yüklenemedi: " + e.getMessage(),
                "Hata",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void removeSelectedFavorite() {
        int selectedRow = favoriteTable.getSelectedRow();
        if (selectedRow >= 0) {
            try {
                String productName = (String) tableModel.getValueAt(selectedRow, 0);
                // Find the product ID by name from the favorites list
                List<Product> favorites = favoriteService.getFavoriteProducts();
                int productId = -1;
                for (Product product : favorites) {
                    if (product.getProductName().equals(productName)) {
                        productId = product.getProductId();
                        break;
                    }
                }
                if (productId != -1) {
                    favoriteService.removeFavorite(productId);
                    tableModel.removeRow(selectedRow);
                    com.bicakclc.ui.InvoicePanel.refreshAllFavoritePanels();
                } else {
                    JOptionPane.showMessageDialog(this, "Favori ürün bulunamadı.", "Hata", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                    "Favoriden kaldırma başarısız: " + e.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public void refresh() {
        loadFavorites();
    }

    // Helper to get Product object at a given row
    private Product getProductAtRow(int row) {
        try {
            String productName = (String) tableModel.getValueAt(row, 0);
            List<Product> favorites = favoriteService.getFavoriteProducts();
            for (Product product : favorites) {
                if (product.getProductName().equals(productName)) {
                    return product;
                }
            }
        } catch (Exception e) {}
        return null;
    }

    // ButtonRenderer and ButtonEditor for 'Faturaya Ekle' button
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setText("Faturaya Ekle");
        }
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Faturaya Ekle");
            return this;
        }
    }
    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private boolean isPushed;
        private FavoritePanel parent;
        public ButtonEditor(JCheckBox checkBox, FavoritePanel parent) {
            super(checkBox);
            this.parent = parent;
            button = new JButton("Faturaya Ekle");
            button.addActionListener(e -> fireEditingStopped());
        }
        public java.awt.Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            isPushed = true;
            return button;
        }
        public Object getCellEditorValue() {
            if (isPushed) {
                int row = favoriteTable.getSelectedRow();
                Product product = parent.getProductAtRow(row);
                if (addToInvoiceListener != null && product != null) {
                    addToInvoiceListener.addProductToInvoice(product);
                }
            }
            isPushed = false;
            return "Faturaya Ekle";
        }
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}