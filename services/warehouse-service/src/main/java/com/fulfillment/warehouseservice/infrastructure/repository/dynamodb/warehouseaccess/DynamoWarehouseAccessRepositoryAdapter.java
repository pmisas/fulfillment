package com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.warehouseaccess;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.warehouseservice.domain.model.WarehouseAccess;
import com.fulfillment.warehouseservice.domain.port.WarehouseAccessRepository;
import com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.WarehouseEntityMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
@Profile("cloud")
public class DynamoWarehouseAccessRepositoryAdapter implements WarehouseAccessRepository {

    private final DynamoDbTable<WarehouseAccessEntity> table;
    private final DynamoDbIndex<WarehouseAccessEntity> warehouseIndex;

    public DynamoWarehouseAccessRepositoryAdapter(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.warehouse-access-table}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(WarehouseAccessEntity.class));
        this.warehouseIndex = table.index(WarehouseAccessEntity.WAREHOUSE_ID_USER_ID_INDEX);
    }

    @Override
    public WarehouseAccess save(WarehouseAccess access) {
        table.putItem(WarehouseEntityMapper.toEntity(access));
        return access;
    }

    @Override
    public Optional<WarehouseAccess> findByUserId(String userId) {
        WarehouseAccessEntity entity = table.getItem(r -> r.key(k -> k.partitionValue(userId)));
        return Optional.ofNullable(entity).map(WarehouseEntityMapper::toDomain);
    }

    @Override
    public List<WarehouseAccess> findActiveByWarehouseId(String warehouseId) {
        return warehouseIndex.query(r -> r.queryConditional(
                QueryConditional.keyEqualTo(k -> k.partitionValue(warehouseId))))
            .stream()
            .flatMap(page -> page.items().stream())
            .filter(WarehouseAccessEntity::isActive)
            .map(WarehouseEntityMapper::toDomain)
            .toList();
    }
}
