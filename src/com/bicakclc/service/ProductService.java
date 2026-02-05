package com.bicakclc.service;

import com.bicakclc.dao.ProductDAO;
import com.bicakclc.model.Product;

import java.sql.SQLException;
import java.util.List;

public class ProductService {
    private ProductDAO productDAO;

    public ProductService() {
        productDAO = new ProductDAO();
    }

    public List<Product> getProductsByCategory(int categoryId) throws SQLException {
        return productDAO.getProductsByCategory(categoryId);
    }

    public List<Product> getAllProducts() throws SQLException {
        return productDAO.getAllProducts();
    }

    public void addProduct(Product product) throws SQLException {
        productDAO.addProduct(product);
    }

    public void deleteProduct(int productId) throws SQLException {
        productDAO.deleteProduct(productId);
    }

    public void updateProduct(Product product) throws SQLException {
        productDAO.updateProduct(product);
    }
}
