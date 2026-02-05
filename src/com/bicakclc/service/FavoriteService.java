// FavoriteService.java
package com.bicakclc.service;

import com.bicakclc.dao.FavoriteDAO;
import com.bicakclc.model.Product;

import java.sql.SQLException;
import java.util.List;

public class FavoriteService {
    private FavoriteDAO favoriteDAO;
    
    public FavoriteService() {
        favoriteDAO = new FavoriteDAO();
    }
    
    public void addFavorite(int productId) throws SQLException {
        favoriteDAO.addFavorite(productId);
    }
    
    public void removeFavorite(int productId) throws SQLException {
        favoriteDAO.removeFavorite(productId);
    }
    
    public List<Product> getFavoriteProducts() throws SQLException {
        return favoriteDAO.getFavoriteProducts();
    }
    
    public boolean isFavorite(int productId) throws SQLException {
        return favoriteDAO.isFavorite(productId);
    }
}