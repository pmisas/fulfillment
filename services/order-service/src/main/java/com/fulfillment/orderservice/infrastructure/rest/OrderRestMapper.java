package com.fulfillment.orderservice.infrastructure.rest;

import com.fulfillment.orderservice.application.dto.CreateOrderCommand;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.infrastructure.rest.dto.CreateOrderRequest;
import com.fulfillment.orderservice.infrastructure.rest.dto.OrderResponse;

public final class OrderRestMapper {
    
    private OrderRestMapper() {}

    public static CreateOrderCommand toCommand(CreateOrderRequest req){
        return new CreateOrderCommand(
            req.customerId(),
            req.items().stream()
                    .map(item -> new CreateOrderCommand.Item(
                item.sku(),
                item.quantity()
            ))
            .toList()
        );
    }

    public static OrderResponse toResponse(Order order) {
        var items = order.getItems().stream()
                    .map(i -> new OrderResponse.Item(i.getSku(), i.getQuantity()))
                    .toList();

        return new OrderResponse(
            order.getOrderId(),
            order.getCustomerId(),
            order.getStatus().name(),
            items
        );
    }


}
