package com.fulfillment.shippingservice.domain.ports;

import java.util.Optional;

import com.fulfillment.shippingservice.domain.model.WarehouseAccess;

public interface WarehouseAccessRepository {

    Optional<WarehouseAccess> findByUserId(String userId);
}
