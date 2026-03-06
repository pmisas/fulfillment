package com.fulfillment.shippingservice.infrastructure.repository.dynamodb.shipment;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.ports.ShipmentRepository;
import com.fulfillment.shippingservice.infrastructure.repository.dynamodb.ShipmentEntityMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("cloud")
public class DynamoShipmentRepositoryAdapter implements ShipmentRepository {

    private final DynamoDbTable<ShipmentEntity> table;

    public DynamoShipmentRepositoryAdapter(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.shipments-table}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(ShipmentEntity.class));
    }

    @Override
    public Shipment save(Shipment shipment) {
        table.putItem(ShipmentEntityMapper.toEntity(shipment));
        return shipment;
    }

    @Override
    public Optional<Shipment> findById(String shipmentId) {
        ShipmentEntity entity = table.getItem(r -> r.key(k -> k.partitionValue(shipmentId)));
        return Optional.ofNullable(entity).map(ShipmentEntityMapper::toDomain);
    }

    @Override
    public List<Shipment> findAll() {
        return table.scan()
                .items()
                .stream()
                .map(ShipmentEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Shipment> findByOrderId(String orderId) {
        return table.scan()
                .items()
                .stream()
                .filter(e -> orderId.equals(e.getOrderId()))
                .map(ShipmentEntityMapper::toDomain)
                .toList();
    }
}
