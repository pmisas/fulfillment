package com.fulfillment.warehouseservice.usecase.operation;

import com.fulfillment.warehouseservice.domain.gateway.WarehouseGateway;
import com.fulfillment.warehouseservice.domain.model.Warehouse;
import reactor.core.publisher.Mono;

public class WarehouseUseCase {

    private final WarehouseGateway warehouseGateway;

    public WarehouseUseCase(WarehouseGateway warehouseGateway) {
        this.warehouseGateway = warehouseGateway;
    }

    public Mono<Warehouse> create(String city, double lat, double lng) {
        return Mono.defer(() -> warehouseGateway.save(Warehouse.create(city, lat, lng)));
    }

    public Mono<Warehouse> getById(String warehouseId) {
        validateWarehouseId(warehouseId);
        return warehouseGateway.findById(warehouseId);
    }

    public Mono<Void> delete(String warehouseId) {
        validateWarehouseId(warehouseId);
        return warehouseGateway.deleteById(warehouseId);
    }

    private void validateWarehouseId(String warehouseId) {
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalArgumentException("warehouseId is required");
        }
    }
}
