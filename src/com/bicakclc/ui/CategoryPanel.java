package com.bicakclc.ui;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;

import com.bicakclc.model.Category;
import com.bicakclc.service.CategoryService;

import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CategoryPanel extends JPanel {
    private JComboBox<String> categoryFilter;
    private JButton showAllCategoriesButton;
    private JDialog categoryDialog;
    private CategoryService categoryService;
    
    public CategoryPanel() {
        setLayout(new BorderLayout());
        categoryService = new CategoryService();
        initializeComponents();
    }
    
    private void initializeComponents() {
        // Filter ComboBox
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        categoryFilter = new JComboBox<>();
        categoryFilter.addItem("All Categories");
        filterPanel.add(new JLabel("Filter: "));
        filterPanel.add(categoryFilter);

        // Add button to show all categories
        showAllCategoriesButton = new JButton("Show All Categories");
        showAllCategoriesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openCategoryDialog();
            }
        });
        filterPanel.add(showAllCategoriesButton);

        add(filterPanel, BorderLayout.NORTH);
        loadCategories();
    }
    
    private void loadCategories() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            // Populate filter combo box
            categoryFilter.removeAllItems();
            categoryFilter.addItem("All Categories");
            for (Category category : categories) {
                categoryFilter.addItem(category.getCategoryName());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load categories: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openCategoryDialog() {
        if (categoryDialog == null) {
            categoryDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Tüm Kategoriler", true);
            categoryDialog.setSize(500, 400);
            categoryDialog.setLocationRelativeTo(this);
            categoryDialog.setLayout(new BorderLayout());
            JTable table = new JTable();
            JScrollPane scrollPane = new JScrollPane(table);
            categoryDialog.add(scrollPane, BorderLayout.CENTER);
            // Load categories into the table
            DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Ad", "Açıklama"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column != 0; // Only allow editing name and description
                }
            };
            try {
                List<Category> categories = categoryService.getAllCategories();
                for (Category category : categories) {
                    model.addRow(new Object[]{
                        category.getCategoryId(),
                        category.getCategoryName(),
                        category.getDescription()
                    });
                }
                table.setModel(model);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Kategoriler yüklenemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
            // Add button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton addButton = new JButton("Ekle");
            JButton deleteButton = new JButton("Sil");
            JButton saveButton = new JButton("Kaydet");
            // Add
            addButton.addActionListener(e -> {
                model.addRow(new Object[]{null, "", ""});
            });
            // Delete 
            deleteButton.addActionListener(e -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    int confirm = JOptionPane.showConfirmDialog(categoryDialog, "Seçili kategoriyi silmek istediğinize emin misiniz?", "Onay", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        Object idObj = model.getValueAt(selectedRow, 0);
                        if (idObj != null) {
                            try {
                                int id = Integer.parseInt(idObj.toString());
                                categoryService.deleteCategory(id);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(categoryDialog, "Silme işlemi başarısız: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                        model.removeRow(selectedRow);
                    }
                } else {
                    JOptionPane.showMessageDialog(categoryDialog, "Lütfen silmek için bir satır seçin.", "Uyarı", JOptionPane.WARNING_MESSAGE);
                }
            });
            // Save 
            saveButton.addActionListener(e -> {
                // Ensure any cell edits are committed
                if (table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }
                try {
                    for (int i = 0; i < model.getRowCount(); i++) {
                        Object idObj = model.getValueAt(i, 0);
                        String name = (String) model.getValueAt(i, 1);
                        String desc = (String) model.getValueAt(i, 2);
                        if (name == null || name.trim().isEmpty()) continue;
                        Category cat = new Category();
                        cat.setCategoryName(name);
                        cat.setDescription(desc);
                        if (idObj == null) {
                            // New category
                            categoryService.addCategory(cat);
                        } else {
                            // Existing category
                            cat.setCategoryId(Integer.parseInt(idObj.toString()));
                            categoryService.updateCategory(cat);
                        }
                    }
                    // Reload table
                    model.setRowCount(0);
                    List<Category> categories = categoryService.getAllCategories();
                    for (Category category : categories) {
                        model.addRow(new Object[]{
                            category.getCategoryId(),
                            category.getCategoryName(),
                            category.getDescription()
                        });
                    }
                    // Refresh the filter combo box
                    loadCategories();
                    JOptionPane.showMessageDialog(categoryDialog, "Kategoriler kaydedildi.", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(categoryDialog, "Kaydetme işlemi başarısız: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                }
            });
            buttonPanel.add(addButton);
            buttonPanel.add(deleteButton);
            buttonPanel.add(saveButton);
            categoryDialog.add(buttonPanel, BorderLayout.SOUTH);
        }
        categoryDialog.setVisible(true);
    }

    public JComboBox<String> getCategoryFilter() {
        return categoryFilter;
    }
}
