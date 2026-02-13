package com.fulfillment.orderservice.infraestructure.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.orderservice.application.OrderService;
import com.fulfillment.orderservice.application.dto.CreateOrderCommand;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.infraestructure.rest.dto.CreateOrderRequest;
import com.fulfillment.orderservice.infraestructure.rest.dto.OrderResponse;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/")
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest req,
                @RequestHeader(value = "Idempotency-Key", required = false) 
                String idempotencyKey) {

        CreateOrderCommand command = OrderRestMapper.toCommand(req);
        Order order = orderService.create(command, idempotencyKey);
        
        return OrderRestMapper.toResponse(order);
    }
    
}
