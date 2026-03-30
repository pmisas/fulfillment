package com.fulfillment.orderstateprocesor.domain.ports;

import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;
import com.fulfillment.orderstateprocesor.domain.model.Status;

import reactor.core.publisher.Mono;

public interface OrderStateTransitionTransaction {

    Mono<Boolean> transitionIfCurrentStatus(
        Order nextOrder,
        Status expectedCurrentStatus,
        OrderStateHistory history
    );
}