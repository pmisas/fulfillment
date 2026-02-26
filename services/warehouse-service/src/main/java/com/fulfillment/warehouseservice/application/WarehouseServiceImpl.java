package com.fulfillment.warehouseservice.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fulfillment.warehouseservice.application.dto.CreateWarehouseCommand;
import com.fulfillment.warehouseservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.domain.port.WarehouseRepository;

@Service
public class WarehouseServiceImpl implements WarehouseService{
    
    private final WarehouseRepository repo;

    public WarehouseServiceImpl(WarehouseRepository repo) {
        this.repo = repo;
    }

    @Override
    public Warehouse create(CreateWarehouseCommand command) {
        Warehouse warehouse = Warehouse.createWarehouse(
                                command.city(), 
                                command.lat(), 
                                command.lng()
        );
        repo.save(warehouse);
        return warehouse;
    }

    @Override
    public Warehouse getById(String warehouseId) {
        return this.repo.findById(warehouseId)
        .orElseThrow(() -> new WarehouseNotFoundException(warehouseId));
    }

    @Override
    public List<Warehouse> getAll() {
        return this.repo.findAll();
    }

    @Override 
    public boolean existsAny() {
        return this.repo.existsAny();
    }

    @Override 
    public boolean existsById(String warehouseId) {
        return this.repo.existsById(warehouseId);
    }

}
