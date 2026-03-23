package com.fulfillment.inventoryservice.infraestrcture.repository.memory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.inventoryservice.domain.model.WarehouseAccess;
import com.fulfillment.inventoryservice.domain.ports.WarehouseAccessRepository;

@Repository
@Profile("local")
public class InMemoryWarehouseAccessRepository implements WarehouseAccessRepository {

    private final ConcurrentMap<String, WarehouseAccess> db = new ConcurrentHashMap<>();

    @Override
    public Optional<WarehouseAccess> findByUserId(String userId) {
        return Optional.ofNullable(db.get(userId));
    }
}
