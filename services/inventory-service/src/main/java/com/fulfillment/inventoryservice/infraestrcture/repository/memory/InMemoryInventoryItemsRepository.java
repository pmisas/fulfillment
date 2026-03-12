package com.fulfillment.inventoryservice.infraestrcture.repository.memory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.domain.ports.InventoryItemsRepository;

@Repository
@Profile("local")
public class InMemoryInventoryItemsRepository implements InventoryItemsRepository {

    // outer key: warehouseId, inner key: sku
    private final Map<String, Map<String, InventoryItem>> store = new ConcurrentHashMap<>();

    @Override
    public InventoryItem save(InventoryItem item) {
        store.computeIfAbsent(item.getWarehouseId(), k -> new ConcurrentHashMap<>())
             .put(item.getSku(), item);
        return item;
    }

    @Override
    public Optional<InventoryItem> findById(String warehouseId, String sku) {
        Map<String, InventoryItem> warehouse = store.get(warehouseId);
        if (warehouse == null) return Optional.empty();
        return Optional.ofNullable(warehouse.get(sku));
    }

    @Override
    public List<InventoryItem> findByWarehouseId(String warehouseId) {
        Map<String, InventoryItem> warehouse = store.get(warehouseId);
        if (warehouse == null) return List.of();
        return List.copyOf(warehouse.values());
    }

    @Override
    public List<InventoryItem> findBySkus(String warehouseId, List<String> skus) {
        Map<String, InventoryItem> warehouse = store.get(warehouseId);
        if (warehouse == null) return List.of();
        return skus.stream()
                .map(warehouse::get)
                .filter(item -> item != null)
                .toList();
    }

    @Override
    public List<InventoryItem> findLowStock(int min) {
        return store.values().stream()
                .flatMap(warehouse -> warehouse.values().stream())
                .filter(item -> item.available() < min)
                .toList();
    }
}
