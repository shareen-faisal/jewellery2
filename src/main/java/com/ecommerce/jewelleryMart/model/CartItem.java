package com.ecommerce.jewelleryMart.model;

public class CartItem {
    private String productId;
    private int quantity;
    private double grams;
    private double finalPrice;

    // Default constructor
    public CartItem() {}

    // Parameterized constructor
    public CartItem(String productId, int quantity, double grams, double finalPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.grams = grams;
        this.finalPrice = finalPrice;
    }

    public double getTotalPrice() {
        return this.finalPrice * this.quantity;
    }

    // --- Getters and Setters ---
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getGrams() { return grams; }
    public void setGrams(double grams) { this.grams = grams; }

    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }
}