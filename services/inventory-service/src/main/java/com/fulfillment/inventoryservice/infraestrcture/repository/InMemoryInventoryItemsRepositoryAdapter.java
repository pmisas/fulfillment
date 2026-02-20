package com.fulfillment.inventoryservice.infraestrcture.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.domain.ports.InventoryItemsRepository;

@Repository
public class InMemoryInventoryItemsRepositoryAdapter implements InventoryItemsRepository{
    
    private final ConcurrentHashMap<String, InventoryItem> store = new ConcurrentHashMap<>();

    @Override
    public InventoryItem save(InventoryItem item) {
        store.put(key(item.getWarehouseId(), item.getSku()), item);
        return item;
    }

    @Override
    public Optional<InventoryItem> findById(String warehouseId, String sku) {
        return Optional.ofNullable(store.get(key(warehouseId, sku)));
    }

    @Override
    public List<InventoryItem> findByWarehouseId(String warehouseId) {
        String prefix = normalize(warehouseId) + "#";
        List<InventoryItem> result = new ArrayList<>();

        for(var entry : store.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.add(entry.getValue());
            }
        }

        return result;
    }

    @Override
    public List<InventoryItem> findLowStock(int min) {
        if (min < 0) throw new IllegalArgumentException("min must be >= 0");

        List<InventoryItem> result = new ArrayList<>();
        for(InventoryItem item : store.values()) {
            if (item.available() <= min) {
                result.add(item);
            }
        }
        return result;
    }

    private String key(String warehouseId, String sku) {
        return normalize(warehouseId) + "#" + normalize(sku);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim();
    }
}
