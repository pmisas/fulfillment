package com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.warehouse;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.domain.port.WarehouseRepository;
import com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.WarehouseEntityMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("cloud")
public class DynamoWarehouseRepositoryAdapter implements WarehouseRepository{
    
    private final DynamoDbTable<WarehouseEntity> table;

    public DynamoWarehouseRepositoryAdapter(
                DynamoDbEnhancedClient enhancedClient,
                @Value("${aws.dynamodb.warehouses-table}") String tableName) {
        this.table = enhancedClient.table(tableName, 
            TableSchema.fromBean(WarehouseEntity.class));
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        table.putItem(WarehouseEntityMapper.toEntity(warehouse));
        return warehouse;
    }

    @Override
    public Optional<Warehouse> findById(String warehouseId) {
        WarehouseEntity entity = table.getItem(r -> r.key(k -> k.partitionValue(warehouseId)));

        return Optional.ofNullable(entity).map(WarehouseEntityMapper::toDomain);
    }

    @Override
    public List<Warehouse> findAll() {
            return table.scan()
            .items()
            .stream()
            .map(WarehouseEntityMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Warehouse> findByCity(String city) {
            return table.scan()
            .items()
            .stream()
            .filter(e -> e.getCity().equals(city))
            .findFirst()
            .map(WarehouseEntityMapper::toDomain);
    }

    @Override
    public boolean existsAny() {
        return table.scan(r -> r.limit(1))
            .items()
            .stream()
            .findAny()
            .isPresent();
    }

    @Override
    public boolean existsById(String warehouseId) {
        WarehouseEntity entity = table.getItem(
            r -> r.key(k -> k.partitionValue(warehouseId))
        );
        return entity != null;
    }


}
