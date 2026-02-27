package com.fulfillment.orderstateprocesor.application;

import com.fulfillment.orderstateprocesor.application.dto.ProcessEventCommand;

import reactor.core.publisher.Mono;

public interface OrderStateProcessorService {
    Mono<Void> process(ProcessEventCommand command);
}
