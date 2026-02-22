package com.fulfillment.orderstateprocesor.application;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderstateprocesor.application.dto.ProcessEventCommand;
import com.fulfillment.orderstateprocesor.domain.exception.OrderNotFoundException;
import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;
import com.fulfillment.orderstateprocesor.domain.model.Status;
import com.fulfillment.orderstateprocesor.domain.ports.InventoryClient;
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateHistoryRepository;
import com.fulfillment.orderstateprocesor.domain.ports.ProcessedEventStore;
import com.fulfillment.orderstateprocesor.domain.ports.WarehouseClient;

@Service
public class OrderStateProcessorServiceImpl implements OrderStateProcessorService {

    private static final Duration EVENT_TTL = Duration.ofDays(7);

    private final ObjectMapper mapper = new ObjectMapper();

    private final OrderRepository orderRepo;
    private final OrderStateHistoryRepository historyRepo;
    private final ProcessedEventStore processedEventStore;

    private final WarehouseClient warehouseClient;
    private final InventoryClient inventoryClient;

    public OrderStateProcessorServiceImpl(
        OrderRepository orderRepo,
        OrderStateHistoryRepository historyRepo,
        ProcessedEventStore processedEventStore,
        WarehouseClient warehouseClient,
        InventoryClient inventoryClient
    ) {
        this.orderRepo = orderRepo;
        this.historyRepo = historyRepo;
        this.processedEventStore = processedEventStore;
        this.warehouseClient = warehouseClient;
        this.inventoryClient = inventoryClient;
    }

    @Override
    public void process(ProcessEventCommand command) {
        boolean firstTime = processedEventStore.putIfAbsent(command.eventId(), EVENT_TTL);
        if (!firstTime) {
            return;
        }

        if ("OrderReceived".equals(command.eventType())) {
            handleOrderReceived(command);
            return;
        }

    }

    private void handleOrderReceived(ProcessEventCommand cmd) {
        String orderId = extractOrderId(cmd);

        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != Status.RECEIVED) {
            return; 
        }

        String selectedWarehouseId = chooseWarehouse(order);

        if (order.getWarehouseId() == null || order.getWarehouseId().isBlank()) {
            order = order.withWarehouse(selectedWarehouseId);
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

        List<String> ids = warehouseClient.listWarehouseIds();
        if (ids.isEmpty()) throw new IllegalStateException("No warehouses available");
        return ids.get(0);
    }

    private void reserveAll(Order order, String warehouseId) {
        order.getItems().forEach(i -> inventoryClient.reserve(warehouseId, i.getSku(), i.getQuantity()));
    }

    private String extractOrderId(ProcessEventCommand cmd) {
        try {
            JsonNode node = mapper.readTree(cmd.payload());
            JsonNode orderIdNode = node.get("orderId");
            if (orderIdNode == null || orderIdNode.asText().isBlank()) {
                throw new IllegalArgumentException("payload missing orderId");
            }
            return orderIdNode.asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid payload JSON: " + e.getMessage(), e);
        }
    }
}
