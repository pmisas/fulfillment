package com.fulfillment.warehouseservice.application;

import java.util.List;

import com.fulfillment.warehouseservice.domain.exception.WarehouseAlreadyExistsException;
import com.fulfillment.warehouseservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.domain.port.WarehouseRepository;

public class WarehouseServiceImpl implements WarehouseService{
    
    private final WarehouseRepository repo;

    public WarehouseServiceImpl(WarehouseRepository repo) {
        this.repo = repo;
    }

    @Override
    public Warehouse create(Warehouse warehouse) {
        this.repo.findByCity(warehouse.getCity())
            .ifPresent(w ->
                {throw new WarehouseAlreadyExistsException(warehouse.getCity());
            });

        return this.repo.save(warehouse);
    }

    @Override
    public Warehouse getById(String id) {
        return this.repo.findById(id)
        .orElseThrow(() -> new WarehouseNotFoundException(id));
    }

    @Override
    public List<Warehouse> getAll() {
        return this.repo.findAll();
    }

}
