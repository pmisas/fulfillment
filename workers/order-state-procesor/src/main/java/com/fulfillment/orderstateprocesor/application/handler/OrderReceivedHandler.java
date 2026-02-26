package com.fulfillment.orderstateprocesor.application.handler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@Component
public class OrderReceivedHandler implements OrderEventHandler {

    private static final double DISTANCE_WEIGHT       = 0.6;
    private static final double STOCK_BUFFER_FACTOR   = 2.0;
    private static final double RESERVATION_THRESHOLD = 0.3;

    private final ObjectMapper mapper;

    private final OrderRepository orderRepo;
    private final OrderStateHistoryRepository historyRepo;
    private final WarehouseClient warehouseClient;
    private final InventoryClient inventoryClient;

    public OrderReceivedHandler(
        ObjectMapper mapper,
        OrderRepository orderRepo,
        OrderStateHistoryRepository historyRepo,
        WarehouseClient warehouseClient,
        InventoryClient inventoryClient
    ) {
        this.mapper = mapper;
        this.orderRepo = orderRepo;
        this.historyRepo = historyRepo;
        this.warehouseClient = warehouseClient;
        this.inventoryClient = inventoryClient;
    }

    @Override
    public String eventType() {
        return "OrderReceived";
    }

    @Override
    public void handle(String payload) {
        OrderReceivedEvent event = parse(payload);
        String orderId = requireNonBlank(event.orderId(), "orderId");

        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != Status.RECEIVED) {
            return;
        }

        String wh = order.getWarehouseId();
        if (wh != null && !wh.isBlank() && warehouseClient.existsById(wh)) {
        } else {
            wh = chooseWarehouse(order);
            order = order.withWarehouse(wh);
            orderRepo.save(order);
        }

        List<InventoryClient.SkuQuantity> skus = order.getItems().stream()
            .map(i -> new InventoryClient.SkuQuantity(i.getSku(), i.getQuantity()))
            .toList();

        String reservationId = "resv:" + order.getOrderId();

        InventoryClient.ReserveResult reserveResult =
            inventoryClient.reserveAll(reservationId, order.getOrderId(), wh, skus);

        if (reserveResult == InventoryClient.ReserveResult.INSUFFICIENT_STOCK) {
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

        List<WarehouseClient.WarehouseSummary> warehouses = warehouseClient.listWarehouses();
        if (warehouses.isEmpty()) throw new IllegalStateException("No warehouses available");

        List<InventoryClient.SkuQuantity> skus = order.getItems().stream()
            .map(i -> new InventoryClient.SkuQuantity(i.getSku(), i.getQuantity()))
            .toList();

        double maxDist = warehouses.stream()
            .mapToDouble(w -> haversine(order.getLat(), order.getLng(), w.lat(), w.lng()))
            .max().orElse(1.0);

        record ScoredWarehouse(String id, double score, boolean isReserved) {}

        List<ScoredWarehouse> candidates = new ArrayList<>();

        for (WarehouseClient.WarehouseSummary w : warehouses) {
            InventoryClient.AvailabilityResult availability =
                inventoryClient.checkAvailability(w.warehouseId(), skus);

            if (!availability.canFulfillAll()) continue;

            double minStockRatio = availability.items().stream()
                .mapToDouble(i -> (double) i.available() / (i.required() * STOCK_BUFFER_FACTOR))
                .min().orElse(0.0);
            double stockScore = Math.min(minStockRatio, 1.0);

            double dist = haversine(order.getLat(), order.getLng(), w.lat(), w.lng());
            double distScore = (maxDist == 0) ? 1.0 : 1.0 - (dist / maxDist);

            double score = DISTANCE_WEIGHT * distScore + (1 - DISTANCE_WEIGHT) * stockScore;
            boolean isReserved = stockScore < RESERVATION_THRESHOLD;

            candidates.add(new ScoredWarehouse(w.warehouseId(), score, isReserved));
        }

        if (candidates.isEmpty()) throw new IllegalStateException("No warehouse can fulfill this order");

        return candidates.stream()
            .filter(c -> !c.isReserved())
            .max(Comparator.comparingDouble(ScoredWarehouse::score))
            .orElseGet(() -> candidates.stream()
                .max(Comparator.comparingDouble(ScoredWarehouse::score))
                .orElseThrow())
            .id();
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private OrderReceivedEvent parse(String json) {
        try {
            return mapper.readValue(json, OrderReceivedEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid OrderReceived payload: " + e.getMessage(), e);
        }
    }
}
