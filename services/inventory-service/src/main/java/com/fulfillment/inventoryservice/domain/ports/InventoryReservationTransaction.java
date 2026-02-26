package com.fulfillment.inventoryservice.domain.ports;

import com.fulfillment.inventoryservice.domain.model.InventoryReservation;

public interface InventoryReservationTransaction {

    ReserveResult reserveAtomically(InventoryReservation reservation);
    void releaseAtomically(String reservationId);
    ConsumeResult consumeAtomically(String reservationId);
    
    enum ReserveResult { RESERVED, ALREADY_RESERVED, INSUFFICIENT_STOCK }
    enum ConsumeResult { CONSUMED, RESERVATION_NOT_FOUND }
}
