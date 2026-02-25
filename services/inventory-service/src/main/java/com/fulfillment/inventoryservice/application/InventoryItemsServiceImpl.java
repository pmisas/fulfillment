package com.fulfillment.inventoryservice.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fulfillment.inventoryservice.application.dto.AvailabilityQuery;
import com.fulfillment.inventoryservice.application.dto.AvailabilityResult;
import com.fulfillment.inventoryservice.application.dto.AvailabilityResult.ItemAvailability;
import com.fulfillment.inventoryservice.application.dto.InventoryCommand;
import com.fulfillment.inventoryservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.domain.ports.InventoryItemsRepository;

@Service
public class InventoryItemsServiceImpl implements InventoryItemsService {

    private final InventoryItemsRepository repo;

    public InventoryItemsServiceImpl(InventoryItemsRepository repo) {
        this.repo = repo;
    }

    @Override
    public InventoryItem consume(InventoryCommand command) {
        InventoryItem current = repo.findById(command.warehouseId(), command.sku())
                                .orElseThrow(() ->
                                    new IllegalArgumentException("Inventory item not found: warehouseId "
                                    + command.warehouseId() + " sku " + command.sku())
                                );
        return repo.save(current.consume(command.amount()));
    }

    @Override
    public InventoryItem restock(InventoryCommand command) {
        String warehouseId = command.warehouseId();
        String sku = command.sku();

        var existing = repo.findById(warehouseId, sku);

        if (existing.isEmpty()) {
            throw new WarehouseNotFoundException(warehouseId);
        }

        return repo.save(existing.get().restock(command.amount()));
    }

    @Override
    public InventoryItem reserve(InventoryCommand command) {
        InventoryItem current = repo.findById(command.warehouseId(), command.sku())
                                .orElseThrow(() ->
                                    new IllegalArgumentException("Inventory item not found: warehouseId "
                                    + command.warehouseId() + " sku " + command.sku())
                                );
        return repo.save(current.reserve(command.amount()));
    }

    @Override
    public InventoryItem release(InventoryCommand command) {
        InventoryItem current = repo.findById(command.warehouseId(), command.sku())
                                .orElseThrow(() ->
                                    new IllegalArgumentException("Inventory item not found: warehouseId "
                                    + command.warehouseId() + " sku " + command.sku())
                                );
        return repo.save(current.release(command.amount()));
    }

    @Override
    public List<InventoryItem> lowStock(int min) {
        return repo.findLowStock(min);
    }

    @Override
    public List<InventoryItem> getByWarehouseId(String warehouseId) {
        return repo.findByWarehouseId(warehouseId);
    }

    @Override
    public AvailabilityResult checkAvailability(AvailabilityQuery query) {
        Map<String, InventoryItem> stockBySku = repo.findByWarehouseId(query.warehouseId())
            .stream()
            .collect(Collectors.toMap(InventoryItem::getSku, i -> i));

        List<ItemAvailability> itemResults = new ArrayList<>();
        boolean canFulfillAll = true;

        for (AvailabilityQuery.SkuQuantity requested : query.items()) {
            InventoryItem stock = stockBySku.get(requested.sku());
            int available = (stock != null) ? stock.available() : 0;
            boolean canFulfill = available >= requested.quantity();


            itemResults.add(new ItemAvailability(
                requested.sku(),
                requested.quantity(),
                available,
                canFulfill
            ));
        }

        return new AvailabilityResult(canFulfillAll, itemResults);
    }
}
