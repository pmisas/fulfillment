package com.fulfillment.orderservice.infrastructure.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.orderservice.application.OrderService;
import com.fulfillment.orderservice.application.dto.CreateOrderCommand;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.infrastructure.rest.dto.AsyncOperationResponse;
import com.fulfillment.orderservice.infrastructure.rest.dto.CreateOrderRequest;
import com.fulfillment.orderservice.infrastructure.rest.dto.OrderResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



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
        @ApiResponse(responseCode = "201", description = "Orden creada correctamente"),
        @ApiResponse(responseCode = "400", description = "Request inválido"),
        @ApiResponse(responseCode = "409", description = "Conflicto de idempotencia"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest req,
                @Parameter(
                description = "Clave de idempotencia para evitar crear la misma orden dos veces",
                required = true,
                example = "order-req-12345"
                )
                @RequestHeader(value = "Idempotency-Key") 
                String idempotencyKey) {

        CreateOrderCommand command = OrderRestMapper.toCommand(req);
        Order order = orderService.create(command, idempotencyKey);
        
        return OrderRestMapper.toResponse(order);
    }

    @Operation(
        summary = "Consultar una orden por ID",
        description = "Devuelve el estado actual de una orden."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden encontrada"),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse getOrderById(
            @Parameter(description = "ID de la orden", required = true, example = "8d91c9aa-1234-4567-890a-abcdef123456")
            @PathVariable("id")String id) {
        Order order = orderService.getById(id);
        return OrderRestMapper.toResponse(order);
    }
    
    @Operation(
        summary = "Solicitar cancelación de una orden",
        description = "Solicita la cancelación asíncrona de una orden. La operación se procesa por eventos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Cancelación solicitada"),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada"),
        @ApiResponse(responseCode = "400", description = "La orden no puede cancelarse")
    })
    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AsyncOperationResponse cancelOrder(
            @Parameter(description = "ID de la orden", required = true, example = "8d91c9aa-1234-4567-890a-abcdef123456")
            @PathVariable("id") String id) {
    
        orderService.cancel(id);
        
        return AsyncOperationResponse.cancellationRequested(id);
    }

}
