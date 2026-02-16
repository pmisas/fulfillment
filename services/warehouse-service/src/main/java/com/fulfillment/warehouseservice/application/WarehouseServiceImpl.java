package com.fulfillment.warehouseservice.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fulfillment.warehouseservice.application.dto.CreateWarehouseCommand;
import com.fulfillment.warehouseservice.domain.exception.WarehouseAlreadyExistsException;
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
        repo.findByCity(command.city())
            .ifPresent(w ->
                {throw new WarehouseAlreadyExistsException(command.city());
            });
        
        Warehouse warehouse = Warehouse.createWarehouse(command.city());
        repo.save(warehouse);
        return warehouse;
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

    @Override 
    public boolean existsAny() {
        return this.repo.existsAny();
    }

}
