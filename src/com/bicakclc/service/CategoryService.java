// CategoryService.java
package com.bicakclc.service;

import com.bicakclc.dao.CategoryDAO;
import com.bicakclc.model.Category;

import java.sql.SQLException;
import java.util.List;

public class CategoryService {
    private CategoryDAO categoryDAO;
    
    public CategoryService() {
        categoryDAO = new CategoryDAO();
    }
    
    public List<Category> getAllCategories() throws SQLException {
        return categoryDAO.getAllCategories();
    }
    
    public List<Category> getFilteredCategories(String searchText, String size) throws SQLException {
        StringBuilder filter = new StringBuilder();
        
        if (!searchText.isEmpty()) {
            filter.append(searchText);
        }
        
        if (!"All".equals(size)) {
            if (filter.length() > 0) {
                filter.append(" AND ");
            }
            filter.append(size);
        }
        
        return categoryDAO.getFilteredCategories(filter.toString());
    }

    public void addCategory(Category category) throws SQLException {
        categoryDAO.addCategory(category);
    }

    public void deleteCategory(int categoryId) throws SQLException {
        categoryDAO.deleteCategory(categoryId);
    }

    public void updateCategory(Category category) throws SQLException {
        categoryDAO.updateCategory(category);
    }
}