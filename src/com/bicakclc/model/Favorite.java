package com.bicakclc.model;

import java.time.LocalDateTime;

public class Favorite {
    private int favoriteId;
    private int productId;
    private LocalDateTime createdDate;
    
	public int getFavoriteId() {
		return favoriteId;
	}
	public void setFavoriteId(int favoriteId) {
		this.favoriteId = favoriteId;
	}
	public int getProductId() {
		return productId;
	}
	public void setProductId(int productId) {
		this.productId = productId;
	}
	
	public LocalDateTime getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}
    
    // Getters and Setters
    
    
}