package com.fulfillment.orderstateprocesor.domain.ports;

import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;

import reactor.core.publisher.Mono;

public interface OrderStateHistoryRepository {
    Mono<Void> append(OrderStateHistory history);
}
