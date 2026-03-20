package com.fulfillment.warehouseservice.infrastructure.repository.memory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.warehouseservice.domain.model.WarehouseAccess;
import com.fulfillment.warehouseservice.domain.port.WarehouseAccessRepository;

@Repository
@Profile("local")
public class InMemoryWarehouseAccessRepository implements WarehouseAccessRepository {

    private final ConcurrentMap<String, WarehouseAccess> db = new ConcurrentHashMap<>();

    @Override
    public WarehouseAccess save(WarehouseAccess access) {
        db.put(access.getUserId(), access);
        return access;
    }

    @Override
    public Optional<WarehouseAccess> findByUserId(String userId) {
        return Optional.ofNullable(db.get(userId));
    }

    @Override
    public List<WarehouseAccess> findActiveByWarehouseId(String warehouseId) {
        return db.values().stream()
            .filter(WarehouseAccess::isActive)
            .filter(access -> access.getWarehouseId().equals(warehouseId))
            .toList();
    }
}
