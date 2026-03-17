package com.fulfillment.inventoryservice.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fulfillment.inventoryservice.application.dto.*;
import com.fulfillment.inventoryservice.application.dto.AvailabilityResult.ItemAvailability;
import com.fulfillment.inventoryservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.domain.model.InventoryReservation;
import com.fulfillment.inventoryservice.domain.ports.InventoryItemsRepository;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction.ConsumeResult;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction.ReserveResult;
import com.fulfillment.inventoryservice.domain.ports.InventoryRestockTransaction;
import com.fulfillment.inventoryservice.domain.ports.WarehouseClient;

@Service
public class InventoryItemsServiceImpl implements InventoryItemsService {

    private static final Logger log = LoggerFactory.getLogger(InventoryItemsServiceImpl.class);

    private final InventoryItemsRepository repo;
    private final InventoryReservationTransaction reservationTx;
    private final InventoryRestockTransaction restockTx;
    private final WarehouseClient warehouseClient;

    public InventoryItemsServiceImpl(
        InventoryItemsRepository repo,
        InventoryReservationTransaction reservationTx,
        InventoryRestockTransaction restockTx,
        WarehouseClient warehouseClient) {
        this.repo = repo;
        this.reservationTx = reservationTx;
        this.restockTx = restockTx;
        this.warehouseClient = warehouseClient;
    }

    @Override
    public ConsumeResult consumeReservation(ConsumeReservationCommand command) {
        return reservationTx.consumeAtomically(command.reservationId());
    }

    @Override
    public List<InventoryItem> restockBatch(RestockBatchCommand command) {
     
        if (!warehouseClient.existsById(command.warehouseId())) {
            throw new WarehouseNotFoundException(command.warehouseId());
        }
        
        Map<String, Integer> summed = new HashMap<>();
        for (var item : command.items()) {
            if (item.quantity() <= 0) throw new IllegalArgumentException("quantity must be > 0");
            summed.merge(item.sku(), item.quantity(), Integer::sum);
        }

        List<InventoryRestockTransaction.Item> txItems = summed.entrySet().stream()
            .map(e -> new InventoryRestockTransaction.Item(e.getKey(), e.getValue()))
            .toList();

        restockTx.restockAtomically(command.warehouseId(), txItems);

        return repo.findByWarehouseId(command.warehouseId());
    }

    @Override
    public List<InventoryItem> lowStock(int min) {
        return repo.findLowStock(min);
    }

    @Override
    public List<InventoryItem> getByWarehouseId(String warehouseId) {
        if (!warehouseClient.existsById(warehouseId)) {
            throw new WarehouseNotFoundException(warehouseId);
        }
        return repo.findByWarehouseId(warehouseId);
    }

    @Override
    public AvailabilityResult checkAvailability(AvailabilityQuery query) {
        List<String> requestedSkus = query.items().stream()
            .map(SkuQuantity::sku)
            .toList();

        Map<String, InventoryItem> stockBySku = repo.findBySkus(query.warehouseId(), requestedSkus)
            .stream()
            .collect(Collectors.toMap(InventoryItem::getSku, i -> i));

        List<ItemAvailability> itemResults = new ArrayList<>();
        boolean canFulfillAll = true;

        for (SkuQuantity requested : query.items()) {
            InventoryItem stock = stockBySku.get(requested.sku());
            int available = (stock != null) ? stock.available() : 0;
            boolean canFulfill = available >= requested.quantity();

            canFulfillAll = canFulfillAll && canFulfill;

            itemResults.add(new ItemAvailability(
                requested.sku(),
                requested.quantity(),
                available,
                canFulfill
            ));
        }

        return new AvailabilityResult(canFulfillAll, itemResults);
    }

    @Override
    public ReserveResult reserveItems(ReserveBatchCommand command) {
        List<InventoryReservation.Item> items = command.items().stream()
            .map(i -> new InventoryReservation.Item(i.sku(), i.quantity()))
            .toList();

        InventoryReservation reservation = InventoryReservation.createInventoryReservation(
            command.reservationId(),
            command.orderId(),
            command.warehouseId(),
            items
        );

        return reservationTx.reserveAtomically(reservation);
    }

    @Override
    public void releaseReservation(String reservationId) {
        log.info("Releasing reservation: reservationId={}", reservationId);
        reservationTx.releaseAtomically(reservationId);
        log.info("Reservation release completed: reservationId={}", reservationId);
    }

}
