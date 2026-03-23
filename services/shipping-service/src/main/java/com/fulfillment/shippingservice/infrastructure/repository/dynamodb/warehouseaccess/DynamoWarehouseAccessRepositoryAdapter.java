package com.fulfillment.shippingservice.infrastructure.repository.dynamodb.warehouseaccess;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.shippingservice.domain.model.WarehouseAccess;
import com.fulfillment.shippingservice.domain.ports.WarehouseAccessRepository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("cloud")
public class DynamoWarehouseAccessRepositoryAdapter implements WarehouseAccessRepository {

    private final DynamoDbTable<WarehouseAccessEntity> table;

    public DynamoWarehouseAccessRepositoryAdapter(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.warehouse-access-table}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(WarehouseAccessEntity.class));
    }

    @Override
    public Optional<WarehouseAccess> findByUserId(String userId) {
        WarehouseAccessEntity entity = table.getItem(r -> r.key(k -> k.partitionValue(userId)));
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(WarehouseAccess.restore(entity.getUserId(), entity.getWarehouseId(), entity.isActive()));
    }
}
