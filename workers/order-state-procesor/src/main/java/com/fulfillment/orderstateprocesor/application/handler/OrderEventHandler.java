package com.fulfillment.orderstateprocesor.application.handler;

import reactor.core.publisher.Mono;

public interface OrderEventHandler {
    String eventType();
    Mono<Void> handle(String payload);
}
