package com.fulfillment.orderstateprocesor.application.handler;

import java.util.Comparator;
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
import com.fulfillment.orderstateprocesor.domain.ports.InventoryClient.ReserveResult;
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateHistoryRepository;
import com.fulfillment.orderstateprocesor.domain.ports.WarehouseClient;
import com.fulfillment.orderstateprocesor.domain.ports.WarehouseClient.WarehouseSummary;
import com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto.OrderReceivedEvent;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

@Component
public class OrderReceivedHandler implements OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderReceivedHandler.class);

    private static final double DISTANCE_WEIGHT       = 0.6;
    private static final double STOCK_BUFFER_FACTOR   = 2.0;
    private static final double RESERVATION_THRESHOLD = 0.3;
    private static final int    MAX_CONCURRENT_CHECKS = 5;

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
    public Mono<Void> handle(String payload) {
        OrderReceivedEvent event = parse(payload);
        String orderId = requireNonBlank(event.orderId(), "orderId");

        return orderRepo.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
            .flatMap(order -> {
                if (order.getStatus() != Status.RECEIVED) {
                    log.info("Order {} already in status {}, skipping processing (idempotency)", 
                            orderId, order.getStatus());
                    return Mono.<Void>empty();
                }
                return rankAndReserveWithFallback(order).then();
            })
            .switchIfEmpty(Mono.empty());
    }

    private Mono<Void> rankAndReserveWithFallback(Order order) {
        List<InventoryClient.SkuQuantity> skus = toSkuQuantities(order);
        String reservationId = "resv:" + order.getOrderId();

        log.info("Processing order={}, items={}", order.getOrderId(), skus);

        return rankWarehouses(order, skus)
            .doOnNext(rankedIds -> log.info("Ranked warehouses for order={}: {}", order.getOrderId(), rankedIds))
            .flatMap(rankedIds -> tryReserveInOrder(rankedIds, reservationId, order, skus))
            .doOnNext(warehouseId -> log.info("tryReserveInOrder succeeded with warehouse={} for order={}", 
                                              warehouseId, order.getOrderId()))
            .flatMap(warehouseId -> persistValidated(order, warehouseId))
            .switchIfEmpty(Mono.defer(() -> {
                log.info("No warehouse found for order={}, executing persistRejected", order.getOrderId());
                return persistRejected(order);
            }));
    }

    private Mono<String> tryReserveInOrder(List<String> rankedWarehouseIds,
                                            String reservationId,
                                            Order order,
                                            List<InventoryClient.SkuQuantity> skus) {
        return Flux.fromIterable(rankedWarehouseIds)
            .concatMap(warehouseId -> {
                log.info("Attempting reserve on warehouse={} for order={}", warehouseId, order.getOrderId());
                return inventoryClient.reserveAll(reservationId, order.getOrderId(), warehouseId, skus)
                    .flatMap(result -> {
                        log.info("Reserve result={} on warehouse={} for order={}", result, warehouseId, order.getOrderId());
                        if (result == ReserveResult.RESERVED || result == ReserveResult.ALREADY_RESERVED) {
                            return Mono.just(warehouseId);
                        }
                        log.info("Reserve INSUFFICIENT_STOCK on warehouse={}, trying next", warehouseId);
                        return Mono.<String>empty();
                    });
            })
            .next();
    }

    private Mono<Void> persistValidated(Order order, String warehouseId) {
        Order assigned = order.getWarehouseId() != null && order.getWarehouseId().equals(warehouseId)
            ? order
            : order.withWarehouse(warehouseId);
        Order validated = assigned.withStatus(Status.VALIDATED);

        return orderRepo.saveIfStatusIs(validated, Status.RECEIVED)
            .flatMap(saved -> {
                if (!saved) {
                    log.info("Order {} already processed by another handler, skipping VALIDATED transition",
                             order.getOrderId());
                    return Mono.just("idempotent").then();
                }
                return historyRepo.append(
                    OrderStateHistory.transition(order.getOrderId(), Status.RECEIVED, Status.VALIDATED))
                    .doOnSuccess(v -> log.info("Order {} VALIDATED with warehouse {}", 
                                               order.getOrderId(), warehouseId));
            });
    }

    private Mono<Void> persistRejected(Order order) {
        Order rejected = order.withStatus(Status.REJECTED);
        return orderRepo.saveIfStatusIs(rejected, Status.RECEIVED)
            .flatMap(saved -> {
                if (!saved) {
                    log.info("Order {} already processed by another handler, skipping REJECTED transition",
                             order.getOrderId());
                    return Mono.just("idempotent").then();
                }
                return historyRepo.append(
                    OrderStateHistory.transition(order.getOrderId(), Status.RECEIVED, Status.REJECTED))
                    .doOnSuccess(v -> log.warn("Order {} REJECTED — no warehouse could reserve stock", 
                                               order.getOrderId()))
                    .then();
            });
    }

    private Mono<List<String>> rankWarehouses(Order order, List<InventoryClient.SkuQuantity> skus) {
        return warehouseClient.listWarehouses()
            .flatMap(warehouses -> {
                log.info("Found {} warehouses for order={}", warehouses.size(), order.getOrderId());
                if (warehouses.isEmpty()) {
                    return Mono.error(new IllegalStateException("No warehouses available"));
                }

                double maxDist = warehouses.stream()
                    .mapToDouble(w -> haversine(order.getLat(), order.getLng(), w.lat(), w.lng()))
                    .max().orElse(1.0);

                return Flux.fromIterable(warehouses)
                    .flatMap(w -> scoreWarehouse(w, order, skus, maxDist), MAX_CONCURRENT_CHECKS)
                    .collectSortedList(Comparator
                        .comparing(ScoredWarehouse::isReserved)
                        .thenComparing(Comparator.comparingDouble(ScoredWarehouse::score).reversed()))
                    .map(scored -> scored.stream().map(ScoredWarehouse::id).toList());
            });
    }

    private Mono<ScoredWarehouse> scoreWarehouse(WarehouseSummary w,
                                                  Order order,
                                                  List<InventoryClient.SkuQuantity> skus,
                                                  double maxDist) {
        double dist = haversine(order.getLat(), order.getLng(), w.lat(), w.lng());
        double distScore = (maxDist == 0) ? 1.0 : 1.0 - (dist / maxDist);

        return inventoryClient.checkAvailability(w.warehouseId(), skus)
            .filter(avail -> {
                boolean canFulfill = avail.canFulfillAll();
                log.info("Warehouse={} canFulfillAll={}", w.warehouseId(), canFulfill);
                return canFulfill;
            })
            .map(avail -> {
                double minStockRatio = avail.items().stream()
                    .mapToDouble(i -> (double) i.available() / (i.required() * STOCK_BUFFER_FACTOR))
                    .min().orElse(0.0);
                double stockScore = Math.min(minStockRatio, 1.0);

                double score = DISTANCE_WEIGHT * distScore + (1 - DISTANCE_WEIGHT) * stockScore;
                boolean nearReservation = stockScore < RESERVATION_THRESHOLD;
                log.info("Warehouse={} score={}, dist={}, stock={}", w.warehouseId(), score, distScore, stockScore);
                return new ScoredWarehouse(w.warehouseId(), score, nearReservation);
            })
            .onErrorResume(ex -> {
                log.warn("Availability check failed for warehouse={}: {}, excluding from candidates",
                    w.warehouseId(), ex.getMessage());
                return Mono.empty();
            });
    }

    private record ScoredWarehouse(String id, double score, boolean isReserved) {}

    private List<InventoryClient.SkuQuantity> toSkuQuantities(Order order) {
        return order.getItems().stream()
            .map(i -> new InventoryClient.SkuQuantity(i.getSku(), i.getQuantity()))
            .toList();
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
