package com.bicakclc.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.bicakclc.model.Product;
import com.bicakclc.util.DatabaseConnection;

public class ProductDAO {

    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE is_active = 1 ORDER BY product_name";
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {
                while(rs.next()) {
                    Product product = mapResultSetToProduct(rs);
                    products.add(product);
                }
        }
        return products;
    }
    
    public List<Product> getProductsByCategory(int categoryId) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE category_id = ? AND is_active = 1 ORDER BY product_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Product product = mapResultSetToProduct(rs);
                    products.add(product);
                }
            }
        }
        return products;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getInt("product_id"));
        product.setCategoryId(rs.getInt("category_id"));
        product.setProductName(rs.getString("product_name"));
        product.setProductCode(rs.getString("product_code"));
        product.setUnit(rs.getString("unit"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setThickness(rs.getBigDecimal("thickness"));
        product.setSizeMm(rs.getInt("size_mm"));
        product.setActive(rs.getBoolean("is_active"));
        product.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime());
        return product;
    }

    public void addProduct(Product product) throws SQLException {
        String sql = "INSERT INTO products (category_id, product_name, product_code, unit, price, thickness, size_mm, is_active, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, 1, GETDATE())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, product.getCategoryId());
            stmt.setString(2, product.getProductName());
            stmt.setString(3, product.getProductCode());
            stmt.setString(4, product.getUnit());
            stmt.setBigDecimal(5, product.getPrice());
            stmt.setBigDecimal(6, product.getThickness());
            stmt.setInt(7, product.getSizeMm());
            stmt.executeUpdate();
        }
    }

    public void deleteProduct(int productId) throws SQLException {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.executeUpdate();
        }
    }

    public void updateProduct(Product product) throws SQLException {
        String sql = "UPDATE products SET category_id = ?, product_name = ?, product_code = ?, unit = ?, price = ?, thickness = ?, size_mm = ?, is_active = ? WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, product.getCategoryId());
            stmt.setString(2, product.getProductName());
            stmt.setString(3, product.getProductCode());
            stmt.setString(4, product.getUnit());
            stmt.setBigDecimal(5, product.getPrice());
            stmt.setBigDecimal(6, product.getThickness());
            stmt.setInt(7, product.getSizeMm());
            stmt.setBoolean(8, product.isActive());
            stmt.setInt(9, product.getProductId());
            stmt.executeUpdate();
        }
    }
}
