// FavoriteDAO.java
package com.bicakclc.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.bicakclc.model.Favorite;
import com.bicakclc.model.Product;
import com.bicakclc.util.DatabaseConnection;

public class FavoriteDAO {
    
    public void addFavorite(int productId) throws SQLException {
        String sql = "INSERT INTO favorites (product_id) VALUES (?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            stmt.executeUpdate();
        }
    }
    
    public void removeFavorite(int productId) throws SQLException {
        String sql = "DELETE FROM favorites WHERE product_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            stmt.executeUpdate();
        }
    }
    
    public List<Product> getFavoriteProducts() throws SQLException {
        List<Product> favorites = new ArrayList<>();
        String sql = """
            SELECT p.*, c.category_name, f.favorite_id 
            FROM products p 
            INNER JOIN favorites f ON p.product_id = f.product_id 
            INNER JOIN categories c ON p.category_id = c.category_id 
            ORDER BY c.category_name, p.product_name
            """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Product product = new Product();
                product.setProductId(rs.getInt("product_id"));
                product.setProductName(rs.getString("product_name"));
                product.setProductCode(rs.getString("product_code"));
                product.setUnit(rs.getString("unit"));
                product.setPrice(rs.getBigDecimal("price"));
                product.setThickness(rs.getBigDecimal("thickness"));
                product.setSizeMm(rs.getInt("size_mm"));
                product.setCategoryId(rs.getInt("category_id"));
                product.setCategoryName(rs.getString("category_name"));
                product.setFavorite(true);
                favorites.add(product);
            }
        }
        return favorites;
    }
    
    public boolean isFavorite(int productId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM favorites WHERE product_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}