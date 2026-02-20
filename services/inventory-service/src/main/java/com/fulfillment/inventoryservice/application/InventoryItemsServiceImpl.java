package com.fulfillment.inventoryservice.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fulfillment.inventoryservice.application.dto.InventoryCommand;
import com.fulfillment.inventoryservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.domain.ports.InventoryItemsRepository;
import com.fulfillment.inventoryservice.domain.ports.WarehouseClient;

@Service
public class InventoryItemsServiceImpl implements InventoryItemsService {
    
    private final InventoryItemsRepository repo;
    private final WarehouseClient warehouseClient;

    public InventoryItemsServiceImpl(InventoryItemsRepository repo, WarehouseClient warehouseClient) {
        this.repo = repo;
        this.warehouseClient = warehouseClient;
    }

    @Override
    public InventoryItem consume(InventoryCommand command) {
        InventoryItem current = repo.findById(command.warehouseId(), command.sku())
                                .orElseThrow(() ->  
                                    new IllegalArgumentException("Inventory item not found: warehouseId "
                                    + command.warehouseId() + "sku "+ command.sku())
                                );
        InventoryItem updated = current.consume(command.amount());
        return repo.save(updated);
    }

    @Override
    public InventoryItem restock(InventoryCommand command){
        String warehouseId = command.warehouseId();
        String sku = command.sku();

        var existing = repo.findById(warehouseId, sku);

        if (existing.isEmpty()) {
            if (!warehouseClient.existsById(warehouseId)) {
                throw new  WarehouseNotFoundException(warehouseId);
            }
            InventoryItem created = InventoryItem.createInventoryItem(warehouseId, sku, 0);

            InventoryItem updated = created.restock(command.amount());
            return repo.save(updated);
        }

        InventoryItem updated = existing.get().restock(command.amount());
        return repo.save(updated);
    }

    @Override
    public InventoryItem reserve(InventoryCommand command) {
        InventoryItem current = repo.findById(command.warehouseId(), command.sku())
                                .orElseThrow(() ->  
                                    new IllegalArgumentException("Inventory item not found: warehouseId "
                                    + command.warehouseId() + "sku "+ command.sku())
                                );
        InventoryItem updated = current.reserve(command.amount());   
        return repo.save(updated);                  
    }

    @Override
    public InventoryItem release(InventoryCommand command) {
        InventoryItem current = repo.findById(command.warehouseId(), command.sku())
                                .orElseThrow(() ->  
                                    new IllegalArgumentException("Inventory item not found: warehouseId "
                                    + command.warehouseId() + "sku "+ command.sku())
                                );
        InventoryItem updated = current.release(command.amount());
        return repo.save(updated);
    }

    @Override
    public List<InventoryItem> lowStock(int min) {
        return repo.findLowStock(min);
    }

    @Override
    public List<InventoryItem> getByWarehouseId(String warehouseId) {
        return repo.findByWarehouseId(warehouseId);
    }
}
