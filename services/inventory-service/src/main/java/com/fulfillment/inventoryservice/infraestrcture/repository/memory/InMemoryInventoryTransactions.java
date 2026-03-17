package com.fulfillment.inventoryservice.infraestrcture.repository.memory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.domain.model.InventoryReservation;
import com.fulfillment.inventoryservice.domain.ports.InventoryItemsRepository;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction;
import com.fulfillment.inventoryservice.domain.ports.InventoryRestockTransaction;

@Component
@Profile("local")
public class InMemoryInventoryTransactions
        implements InventoryReservationTransaction, InventoryRestockTransaction {

    private final InventoryItemsRepository itemsRepository;
    private final Map<String, InventoryReservation> reservations = new ConcurrentHashMap<>();

    public InMemoryInventoryTransactions(InventoryItemsRepository itemsRepository) {
        this.itemsRepository = itemsRepository;
    }

    @Override
    public synchronized ReserveResult reserveAtomically(InventoryReservation reservation) {
        if (reservations.containsKey(reservation.getReservationId())) {
            return ReserveResult.ALREADY_RESERVED;
        }

        for (var item : reservation.getItems()) {
            Optional<InventoryItem> existing = itemsRepository.findById(reservation.getWarehouseId(), item.sku());
            if (existing.isEmpty() || existing.get().available() < item.quantity()) {
                return ReserveResult.INSUFFICIENT_STOCK;
            }
        }

        for (var item : reservation.getItems()) {
            InventoryItem existing = itemsRepository.findById(reservation.getWarehouseId(), item.sku()).get();
            itemsRepository.save(existing.reserve(item.quantity()));
        }

        reservations.put(reservation.getReservationId(), reservation);
        return ReserveResult.RESERVED;
    }

    @Override
    public synchronized void releaseAtomically(String reservationId) {
        InventoryReservation reservation = reservations.get(reservationId);
        if (reservation == null) return;

        for (var item : reservation.getItems()) {
            itemsRepository.findById(reservation.getWarehouseId(), item.sku())
                    .ifPresent(existing -> itemsRepository.save(existing.release(item.quantity())));
        }

        reservations.remove(reservationId);
    }

    @Override
    public synchronized ConsumeResult consumeAtomically(String reservationId) {
        InventoryReservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            return ConsumeResult.RESERVATION_NOT_FOUND;
        }

        for (var item : reservation.getItems()) {
            itemsRepository.findById(reservation.getWarehouseId(), item.sku())
                    .ifPresent(existing -> itemsRepository.save(existing.consume(item.quantity())));
        }

        reservations.remove(reservationId);
        return ConsumeResult.CONSUMED;
    }

    @Override
    public synchronized void restockAtomically(String warehouseId, List<Item> items) {
        for (var item : items) {
            Optional<InventoryItem> existing = itemsRepository.findById(warehouseId, item.sku());
            if (existing.isPresent()) {
                itemsRepository.save(existing.get().restock(item.quantity()));
            } else {
                itemsRepository.save(InventoryItem.createInventoryItem(warehouseId, item.sku(), item.quantity()));
            }
        }
    }
}
