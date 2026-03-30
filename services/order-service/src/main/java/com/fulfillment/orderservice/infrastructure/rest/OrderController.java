package com.fulfillment.orderservice.infrastructure.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.orderservice.application.OrderService;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.Status;
import com.fulfillment.orderservice.infrastructure.rest.dto.ApiErrorResponse;
import com.fulfillment.orderservice.infrastructure.rest.dto.AsyncOperationResponse;
import com.fulfillment.orderservice.infrastructure.rest.dto.CreateOrderRequest;
import com.fulfillment.orderservice.infrastructure.rest.dto.OrderResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Operaciones sobre órdenes")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(
        summary = "Crear una orden",
        description = "Crea una orden nueva usando Idempotency-Key para evitar duplicados."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Orden creada correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Request con campos inválidos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Conflicto de idempotencia: otra creación en progreso con la misma clave, o estado inconsistente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @Valid @RequestBody CreateOrderRequest req,
            @Parameter(
                description = "Clave de idempotencia para evitar crear la misma orden dos veces",
                required = true,
                example = "order-req-12345"
            )
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication auth) {

        List<OrderService.OrderItemInput> items = req.items().stream()
            .map(item -> new OrderService.OrderItemInput(item.sku(), item.quantity()))
            .toList();

        Order order = orderService.create(
            auth.getName(),
            req.lat(),
            req.lng(),
            items,
            idempotencyKey
        );

        return OrderRestMapper.toResponse(order);
    }

    @Operation(
        summary = "Consultar una orden por ID",
        description = "Devuelve el estado actual de una orden."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "403", description = "El operador autenticado no tiene permisos para consultar esta orden",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse getOrderById(
            @Parameter(description = "ID de la orden", required = true, example = "8d91c9aa-1234-4567-890a-abcdef123456")
            @PathVariable("id") String id,
            Authentication auth) {
        Order order = orderService.getById(id, auth.getName(), isAdmin(auth));
        return OrderRestMapper.toResponse(order);
    }

    @Operation(
        summary = "Solicitar cancelación de una orden",
        description = "Solicita la cancelación asíncrona de una orden. La operación se procesa por eventos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Cancelación solicitada. La orden será cancelada de forma asíncrona por el worker",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AsyncOperationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Transición de estado inválida: la orden está en estado SHIPPED y no puede cancelarse",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "El operador autenticado no tiene permisos para cancelar esta orden",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AsyncOperationResponse cancelOrder(
            @Parameter(description = "ID de la orden", required = true, example = "8d91c9aa-1234-4567-890a-abcdef123456")
            @PathVariable("id") String id,
            Authentication auth) {

        orderService.cancel(id, auth.getName(), isAdmin(auth));
        return AsyncOperationResponse.cancellationRequested(id);
    }

    @Operation(summary = "Listar órdenes", description = "ADMIN ve todas las órdenes. OPERATOR ve solo las suyas. Soporta filtros opcionales por estado o warehouse, pero no ambos a la vez.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de órdenes",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Filtros inválidos o incompatibles",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/mine")
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponse> getMyOrders(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String warehouseId,
            Authentication auth) {
        if (status != null && warehouseId != null) {
            throw new IllegalArgumentException("Cannot filter by both status and warehouseId simultaneously");
        }

        String requesterId = auth.getName();
        boolean admin = isAdmin(auth);
        List<Order> orders;

        if (status != null) {
            orders = orderService.listByStatus(status, requesterId, admin);
        } else if (warehouseId != null) {
            orders = orderService.listByWarehouse(warehouseId, requesterId, admin);
        } else {
            orders = orderService.listAll(requesterId, admin);
        }

        return orders.stream()
            .map(OrderRestMapper::toResponse)
            .toList();
    }

    @Operation(summary = "Listar órdenes por estado", description = "ADMIN ve todas. OPERATOR ve solo las suyas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de órdenes",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Estado inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/by-status/{status}")
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponse> getByStatus(
            @Parameter(description = "Estado de la orden", required = true, example = "VALIDATED")
            @PathVariable Status status,
            Authentication auth) {
        return orderService.listByStatus(status, auth.getName(), isAdmin(auth))
            .stream()
            .map(OrderRestMapper::toResponse)
            .toList();
    }

    @Operation(summary = "Listar órdenes por warehouse", description = "ADMIN ve todas. OPERATOR ve solo las suyas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de órdenes",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/by-warehouse/{warehouseId}")
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponse> getByWarehouse(
            @Parameter(description = "ID del warehouse", required = true, example = "wh-1")
            @PathVariable String warehouseId,
            Authentication auth) {
        return orderService.listByWarehouse(warehouseId, auth.getName(), isAdmin(auth))
            .stream()
            .map(OrderRestMapper::toResponse)
            .toList();
    }

    @Operation(summary = "Listar órdenes por operador", description = "Solo ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de órdenes",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/by-operator/{operatorId}")
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponse> getByOperator(
            @Parameter(description = "ID del operador", required = true, example = "user-sub-123")
            @PathVariable String operatorId,
            Authentication auth) {
        return orderService.listByOperator(operatorId, auth.getName(), isAdmin(auth))
            .stream()
            .map(OrderRestMapper::toResponse)
            .toList();
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
