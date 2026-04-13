package com.fulfillment.warehouseservice.infrastructure.drivenadapters.warehouserepository;

import com.fulfillment.warehouseservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.warehouseservice.domain.gateway.WarehouseGateway;
import com.fulfillment.warehouseservice.domain.model.Warehouse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Repository
public class WarehouseRepositoryAdapter implements WarehouseGateway {

    private final Map<String, WarehouseEntity> warehouses = new ConcurrentHashMap<>();
    private final WarehouseEntityMapper mapper = new WarehouseEntityMapper();

    @Override
    public Mono<Warehouse> save(Warehouse warehouse) {
        return Mono.fromCallable(() -> {
            WarehouseEntity entity = mapper.toEntity(warehouse);
            warehouses.put(entity.getWarehouseId(), entity);
            return mapper.toDomain(entity);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Warehouse> findById(String warehouseId) {
        return Mono.fromCallable(() -> {
            WarehouseEntity entity = warehouses.get(warehouseId);
            if (entity == null) {
                throw new WarehouseNotFoundException(warehouseId);
            }
            return mapper.toDomain(entity);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> deleteById(String warehouseId) {
        return Mono.fromRunnable(() -> {
            WarehouseEntity removed = warehouses.remove(warehouseId);
            if (removed == null) {
                throw new WarehouseNotFoundException(warehouseId);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
