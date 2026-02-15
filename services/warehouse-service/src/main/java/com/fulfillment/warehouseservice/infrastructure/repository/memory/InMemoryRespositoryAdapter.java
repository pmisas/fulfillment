package com.fulfillment.warehouseservice.infrastructure.repository.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.domain.port.WarehouseRepository;

@Repository
public class InMemoryRespositoryAdapter implements WarehouseRepository {
    
    private final ConcurrentMap<String, Warehouse> db = new ConcurrentHashMap<>();
    
    @Override
    public Warehouse save(Warehouse warehouse) {
        db.put(warehouse.getWarehouseId(), warehouse);
        return warehouse;
    }

    @Override
    public Optional<Warehouse> findById(String warehouseId) {
        return Optional.ofNullable(db.get(warehouseId));
    }

    @Override
    public List<Warehouse> findAll(){
        return new ArrayList<>(db.values());
    }

    @Override
    public Optional<Warehouse> findByCity(String city) {
        return db.values()
             .stream()
             .filter(w -> w.getCity().equalsIgnoreCase(city))
             .findFirst();
    }
}
