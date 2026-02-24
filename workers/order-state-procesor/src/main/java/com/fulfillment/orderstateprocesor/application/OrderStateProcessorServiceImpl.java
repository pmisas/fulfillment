package com.fulfillment.orderstateprocesor.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderstateprocesor.application.dto.ProcessEventCommand;
import com.fulfillment.orderstateprocesor.domain.exception.OrderNotFoundException;
import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;
import com.fulfillment.orderstateprocesor.domain.model.Status;
import com.fulfillment.orderstateprocesor.domain.ports.InventoryClient;
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateHistoryRepository;
import com.fulfillment.orderstateprocesor.domain.ports.WarehouseClient;
import com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto.OrderReceivedEvent;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

@Service
public class OrderStateProcessorServiceImpl implements OrderStateProcessorService {

    private final ObjectMapper mapper = new ObjectMapper();

    private final OrderRepository orderRepo;
    private final OrderStateHistoryRepository historyRepo;

    private final WarehouseClient warehouseClient;
    private final InventoryClient inventoryClient;

    public OrderStateProcessorServiceImpl(
        OrderRepository orderRepo,
        OrderStateHistoryRepository historyRepo,
        WarehouseClient warehouseClient,
        InventoryClient inventoryClient
    ) {
        this.orderRepo = orderRepo;
        this.historyRepo = historyRepo;
        this.warehouseClient = warehouseClient;
        this.inventoryClient = inventoryClient;
    }

    @Override
    public void process(ProcessEventCommand command) {
        if ("OrderReceived".equals(command.eventType())) {
            OrderReceivedEvent event = parseOrderReceived(command.payload());
            handleOrderReceived(event);
            return;
        }

    }

    private void handleOrderReceived(OrderReceivedEvent event) {
        String orderId = requireNonBlank(event.orderId(), "orderId");

        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

            if (order.getStatus() != Status.RECEIVED) {
                return; 
        }

        String selectedWarehouseId = chooseWarehouse(order);

        if (order.getWarehouseId() == null || order.getWarehouseId().isBlank()) {
            order = order.withWarehouse(selectedWarehouseId);
            orderRepo.save(order);
        }

        try {
            reserveAll(order, selectedWarehouseId);
        } catch (Exception ex) {
            Order rejected = order.withStatus(Status.REJECTED);
            orderRepo.save(rejected);
            historyRepo.append(OrderStateHistory.transition(order.getOrderId(), Status.RECEIVED, Status.REJECTED));
            return;
        }

        Order validated = order.withStatus(Status.VALIDATED);
        orderRepo.save(validated);
        historyRepo.append(OrderStateHistory.transition(order.getOrderId(), Status.RECEIVED, Status.VALIDATED));
    }

    private String chooseWarehouse(Order order) {
        String existing = order.getWarehouseId();
        if (existing != null && !existing.isBlank() && warehouseClient.existsById(existing)) {
            return existing;
        }

        List<String> ids = warehouseClient.listWarehouses();
        if (ids.isEmpty()) throw new IllegalStateException("No warehouses available");
        return ids.get(0);
    }

    private void reserveAll(Order order, String warehouseId) {
        order.getItems().forEach(i -> inventoryClient.reserve(warehouseId, i.getSku(), i.getQuantity()));
    }

    private OrderReceivedEvent parseOrderReceived(String json) {
        try {
            return mapper.readValue(json, OrderReceivedEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid OrderReceived payload: " + e.getMessage(), e);
        }
    }
    
}
