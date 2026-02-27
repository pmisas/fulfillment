package com.fulfillment.inventoryservice.infraestrcture.repository.dynamodb.inventoryItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.domain.ports.InventoryItemsRepository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;

@Repository
@Profile("cloud")
public class DynamoInventoryItemsRepositoryAdapter implements InventoryItemsRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<InventoryItemEntity> table;

    public DynamoInventoryItemsRepositoryAdapter(
        DynamoDbEnhancedClient enhancedClient,
        @Value("${aws.dynamodb.inventory-table}") String tableName
    ) {
        this.enhancedClient = enhancedClient;
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(InventoryItemEntity.class));
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        table.putItem(toEntity(item));
        return item;
    }

    @Override
    public Optional<InventoryItem> findById(String warehouseId, String sku) {
        InventoryItemEntity e = table.getItem(r -> r.key(k ->
            k.partitionValue(warehouseId).sortValue(sku)
        ));
        return Optional.ofNullable(e).map(this::toDomain);
    }

    @Override
    public List<InventoryItem> findByWarehouseId(String warehouseId) {
        List<InventoryItem> result = new ArrayList<>();

        table.query(r -> r.queryConditional(
                QueryConditional.keyEqualTo(k -> k.partitionValue(warehouseId))
            ))
            .stream()
            .flatMap(page -> page.items().stream())
            .map(this::toDomain)
            .forEach(result::add);

        return result;
    }

    @Override
    public List<InventoryItem> findBySkus(String warehouseId, List<String> skus) {
        if (skus == null || skus.isEmpty()) return List.of();

        ReadBatch.Builder<InventoryItemEntity> readBatchBuilder = ReadBatch.builder(InventoryItemEntity.class)
            .mappedTableResource(table);

        for (String sku : skus) {
            readBatchBuilder.addGetItem(r -> r.key(k -> k.partitionValue(warehouseId).sortValue(sku)));
        }

        BatchGetItemEnhancedRequest request = BatchGetItemEnhancedRequest.builder()
            .readBatches(readBatchBuilder.build())
            .build();

        return enhancedClient.batchGetItem(request)
            .resultsForTable(table)
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<InventoryItem> findLowStock(int min) {
        if (min < 0) throw new IllegalArgumentException("min must be >= 0");

        List<InventoryItem> result = new ArrayList<>();

        table.scan()
            .stream()
            .flatMap(page -> page.items().stream())
            .map(this::toDomain)
            .filter(i -> i.available() <= min)
            .forEach(result::add);

        return result;
    }

    private InventoryItemEntity toEntity(InventoryItem item) {
        InventoryItemEntity e = new InventoryItemEntity();
        e.setWarehouseId(item.getWarehouseId());
        e.setSku(item.getSku());
        e.setQuantity(item.getQuantity());
        e.setReserved(item.getReserved());
        e.setUpdatedAt(item.getUpdatedAt());
        return e;
    }

    private InventoryItem toDomain(InventoryItemEntity e) {
        return InventoryItem.restore(
            e.getWarehouseId(),
            e.getSku(),
            e.getQuantity() == null ? 0 : e.getQuantity(),
            e.getReserved() == null ? 0 : e.getReserved(),
            e.getUpdatedAt()
        );
    }
}
