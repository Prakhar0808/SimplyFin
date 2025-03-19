package com.example.fintechapp;

public class Item {
    private String itemId;
    private String itemName;
    private int itemQuantity;
    private double perItemPrice;
    private double totalPrice;
    private String date;
    private String lastModified;

    public Item() {
        // Default constructor required for Firebase
    }

    public Item(String itemId, String itemName, int itemQuantity, double perItemPrice, double totalPrice, String date, String lastModified) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemQuantity = itemQuantity;
        this.perItemPrice = perItemPrice;
        this.totalPrice = totalPrice;
        this.date = date;
        this.lastModified = lastModified;
    }

    // Getters and Setters for all fields
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public int getItemQuantity() { return itemQuantity; }
    public void setItemQuantity(int itemQuantity) { this.itemQuantity = itemQuantity; }

    public double getPerItemPrice() { return perItemPrice; }
    public void setPerItemPrice(double perItemPrice) { this.perItemPrice = perItemPrice; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }
}
