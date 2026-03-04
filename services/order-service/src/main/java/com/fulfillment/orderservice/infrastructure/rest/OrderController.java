package com.fulfillment.orderservice.infrastructure.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.orderservice.application.OrderService;
import com.fulfillment.orderservice.application.dto.CreateOrderCommand;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.infrastructure.rest.dto.CreateOrderRequest;
import com.fulfillment.orderservice.infrastructure.rest.dto.OrderResponse;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest req,
                @RequestHeader(value = "Idempotency-Key") 
                String idempotencyKey) {

        CreateOrderCommand command = OrderRestMapper.toCommand(req);
        Order order = orderService.create(command, idempotencyKey);
        
        return OrderRestMapper.toResponse(order);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse getOrderById(@PathVariable("id")String id) {
        Order order = orderService.getById(id);
        return OrderRestMapper.toResponse(order);
    }
    
    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse cancelOrder(@PathVariable("id") String id) {
    
        Order order = orderService.cancel(id);
        return OrderRestMapper.toResponse(order);
    }

}
