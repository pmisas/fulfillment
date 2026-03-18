package com.fulfillment.notificationlambda.domain.ports;

import com.fulfillment.notificationlambda.domain.model.OrderInfo;

import java.util.Optional;

public interface OrderLookup {
    Optional<OrderInfo> findById(String orderId);
}
