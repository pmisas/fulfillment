package com.fulfillment.orderstateprocesor.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fulfillment.orderstateprocesor.application.dto.ProcessEventCommand;
import com.fulfillment.orderstateprocesor.application.handler.OrderEventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@Service
public class OrderStateProcessorServiceImpl implements OrderStateProcessorService {

    private static final Logger log = LoggerFactory.getLogger(OrderStateProcessorServiceImpl.class);

    private final Map<String, OrderEventHandler> handlers;

    public OrderStateProcessorServiceImpl(List<OrderEventHandler> handlerList) {
        this.handlers = handlerList.stream()
            .collect(Collectors.toMap(OrderEventHandler::eventType, h -> h));
    }

    @Override
    public Mono<Void> process(ProcessEventCommand command) {
        OrderEventHandler handler = handlers.get(command.eventType());
        if (handler == null) {
            log.warn("No handler registered for event type: {}", command.eventType());
            return Mono.empty();
        }
        return handler.handle(command.payload());
    }
}
