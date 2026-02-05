package com.bicakclc.ui;

import javax.swing.*;

import com.bicakclc.model.Category;
import com.bicakclc.model.Product;
import com.bicakclc.service.CategoryService;
import com.bicakclc.service.ProductService;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class MainWindow extends JFrame {
    private CategoryPanel categoryPanel;
    private ProductPanel productPanel;
    private FavoritePanel favoritePanel;
    private InvoicePanel invoicePanel;
    private InvoiceViewPanel invoiceViewPanel;
    private CustomerPanel customerPanel;
    private CategoryService categoryService;
    private ProductService productService;
    
    public MainWindow() {
        setTitle("Bıçak Hesaplama");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        initializeComponents();
        layoutComponents();
    }
    
    private void initializeComponents() {
        categoryPanel = new CategoryPanel();
        favoritePanel = new FavoritePanel();
        productPanel = new ProductPanel(favoritePanel);
        invoicePanel = new InvoicePanel();
        invoiceViewPanel = new InvoiceViewPanel();
        customerPanel = new CustomerPanel();
        productService = new ProductService();
        categoryService = new CategoryService();
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Product management tab
        JPanel productManagementPanel = new JPanel(new BorderLayout());
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(categoryPanel, BorderLayout.NORTH);
        leftPanel.add(productPanel, BorderLayout.CENTER);
        leftPanel.add(favoritePanel, BorderLayout.SOUTH);
        
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        productManagementPanel.add(leftPanel, BorderLayout.WEST);
        productManagementPanel.add(rightPanel, BorderLayout.CENTER);
        
        tabbedPane.addTab("Ürün Yönetimi", productManagementPanel);
        tabbedPane.addTab("Fatura Oluşturma", invoicePanel);
        tabbedPane.addTab("Fatura Görüntüleme", invoiceViewPanel);
        tabbedPane.addTab("Müşteri Yönetimi", customerPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Add category filter listener
        JComboBox<String> categoryFilter = categoryPanel.getCategoryFilter();
        categoryFilter.addActionListener(e -> {
            String selected = (String) categoryFilter.getSelectedItem();
            if (selected != null && !selected.equals("All Categories")) {
                try {
                    List<Category> categories = categoryService.getAllCategories();
                    for (Category cat : categories) {
                        if (cat.getCategoryName().equals(selected)) {
                            List<Product> products = productService.getProductsByCategory(cat.getCategoryId());
                            productPanel.updateProducts(products);
                            break;
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to load products: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                try {
                    List<Product> products = productService.getAllProducts();
                    productPanel.updateProducts(products);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to load products: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}
