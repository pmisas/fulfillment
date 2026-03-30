package com.fulfillment.orderstateprocesor.application.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderstateprocesor.domain.exception.OrderNotFoundException;
import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;
import com.fulfillment.orderstateprocesor.domain.model.Status;
import com.fulfillment.orderstateprocesor.domain.ports.InventoryClient;
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateTransitionTransaction;
import com.fulfillment.orderstateprocesor.domain.ports.ShippingClient;
import com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto.WarehouseOrderActionEvent;

import reactor.core.publisher.Mono;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

@Component
public class OrderPackedHandler implements OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderPackedHandler.class);

    private final ObjectMapper mapper;
    private final OrderRepository orderRepo;
    private final OrderStateTransitionTransaction transitionTx;
    private final InventoryClient inventoryClient;
    private final ShippingClient shippingClient;

    public OrderPackedHandler(
        ObjectMapper mapper,
        OrderRepository orderRepo,
        OrderStateTransitionTransaction transitionTx,
        InventoryClient inventoryClient,
        ShippingClient shippingClient
    ) {
        this.mapper = mapper;
        this.orderRepo = orderRepo;
        this.transitionTx = transitionTx;
        this.inventoryClient = inventoryClient;
        this.shippingClient = shippingClient;
    }

    @Override
    public String eventType() {
        return "PackingCompleted";
    }

    @Override
    public Mono<Void> handle(String payload) {
        WarehouseOrderActionEvent event = parse(payload);
        String orderId = requireNonBlank(event.orderId(), "orderId");
        String warehouseId = requireNonBlank(event.warehouseId(), "warehouseId");

        return orderRepo.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
            .flatMap(order -> applyOrderPacked(order, warehouseId));
    }

    private Mono<Void> applyOrderPacked(Order order, String warehouseId) {
        if (order.getStatus() == Status.PACKED
                || order.getStatus() == Status.SHIPPED || order.getStatus() == Status.CANCELED) {
            return Mono.empty();
        }

        if (order.getStatus() != Status.PICKED) {
            log.warn("Ignoring OrderPacked for order {} because status is {}", order.getOrderId(), order.getStatus());
            return Mono.empty();
        }

        if (order.getWarehouseId() == null || order.getWarehouseId().isBlank()
                || !order.getWarehouseId().equals(warehouseId)) {
            log.warn("Ignoring OrderPacked for order {} because warehouse mismatch orderWh={} eventWh={}",
                order.getOrderId(), order.getWarehouseId(), warehouseId);
            return Mono.empty();
        }

        String reservationId = "resv:" + order.getOrderId();

        List<ShippingClient.ShipmentItemDto> shipmentItems = order.getItems().stream()
            .map(i -> new ShippingClient.ShipmentItemDto(i.getSku(), i.getQuantity()))
            .toList();

        Order next = order.withStatus(Status.PACKED);
        OrderStateHistory history = OrderStateHistory.transition(order.getOrderId(), Status.PICKED, Status.PACKED);

        return inventoryClient.consumeReservation(reservationId)
            .doOnNext(result -> log.info("Reservation {} consume result for order {}: {}",
                reservationId, order.getOrderId(), result))
            .flatMap(result -> {
                if (result != InventoryClient.ConsumeResult.CONSUMED
                        && result != InventoryClient.ConsumeResult.RESERVATION_NOT_FOUND) {
                    return Mono.error(new IllegalStateException(
                        "Reservation " + reservationId + " was not consumed for order " + order.getOrderId()
                    ));
                }
                return shippingClient.createShipment(order.getOrderId(), order.getWarehouseId(), shipmentItems);
            })
            .then(transitionTx.transitionIfCurrentStatus(next, Status.PICKED, history))
            .flatMap(saved -> {
                if (!saved) {
                    log.info("Order {} already advanced past PICKED (concurrent message), skipping PACKED transition",
                        order.getOrderId());
                    return Mono.empty();
                }
                log.info("Order {} -> PACKED (warehouse={})", order.getOrderId(), warehouseId);
                return Mono.empty();
            });
    }

    private WarehouseOrderActionEvent parse(String json) {
        try {
            return mapper.readValue(json, WarehouseOrderActionEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid PackingStarted payload: " + e.getMessage(), e);
        }
    }
}
