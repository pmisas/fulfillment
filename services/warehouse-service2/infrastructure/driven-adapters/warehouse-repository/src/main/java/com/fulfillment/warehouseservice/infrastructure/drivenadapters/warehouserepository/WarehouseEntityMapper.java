package com.fulfillment.warehouseservice.infrastructure.drivenadapters.warehouserepository;

import com.fulfillment.warehouseservice.domain.model.Warehouse;

public class WarehouseEntityMapper {

    public WarehouseEntity toEntity(Warehouse warehouse) {
        return new WarehouseEntity(
                warehouse.getWarehouseId(),
                warehouse.getCity(),
                warehouse.getLat(),
                warehouse.getLng(),
                warehouse.getCreatedAt()
        );
    }

    public Warehouse toDomain(WarehouseEntity entity) {
        return new Warehouse(
                entity.getWarehouseId(),
                entity.getCity(),
                entity.getLat(),
                entity.getLng(),
                entity.getCreatedAt()
        );
    }
}
