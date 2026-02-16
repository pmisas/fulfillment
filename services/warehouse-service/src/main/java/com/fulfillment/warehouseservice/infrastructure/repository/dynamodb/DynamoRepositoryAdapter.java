package com.fulfillment.warehouseservice.infrastructure.repository.dynamodb;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.domain.port.WarehouseRepository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("dynamo")
public class DynamoRepositoryAdapter implements WarehouseRepository{
    
    private final DynamoDbTable<WarehouseEntity> table;

    public DynamoRepositoryAdapter(
                DynamoDbEnhancedClient enhancedClient,
                @Value("${aws.dynamodb.table}") String tableName) {
        this.table = enhancedClient.table(tableName, 
            TableSchema.fromBean(WarehouseEntity.class));
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        table.putItem(toEntity(warehouse));
        return warehouse;
    }

    @Override
    public Optional<Warehouse> findById(String warehouseId) {
        WarehouseEntity entity = table.getItem(r -> r.key(k -> k.partitionValue(warehouseId)));

        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<Warehouse> findAll() {
            return table.scan()
            .items()
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public Optional<Warehouse> findByCity(String city) {
            return table.scan()
            .items()
            .stream()
            .filter(e -> e.getCity().equals(city))
            .findFirst()
            .map(this::toDomain);
    }

    @Override
    public boolean existsAny() {
            return table.scan()
            .items()
            .stream()
            .findAny()
            .isPresent();
    }

    private WarehouseEntity toEntity(Warehouse warehouse) {
        WarehouseEntity e = new WarehouseEntity();
        e.setWarehouseId(warehouse.getWarehouseId());
        e.setCity(warehouse.getCity());
        e.setCreatedAt(warehouse.getCreatedAt());

        return e;
    }

    private Warehouse toDomain(WarehouseEntity e) {
        return Warehouse.restore(
            e.getWarehouseId(),
            e.getCity(),
            e.getCreatedAt()
        );
    }

}
