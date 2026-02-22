package com.fulfillment.orderstateprocesor.domain.ports;

public interface InventoryClient {
    void reserve(String warehouseId, String sku, int amount);
    void release(String warehouseId, String sku, int amount);
}