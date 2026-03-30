package com.fulfillment.shippingservice.infrastructure.rest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.shippingservice.application.ShippingService;
import com.fulfillment.shippingservice.application.dto.CreateShipmentCommand;
import com.fulfillment.shippingservice.domain.model.CarrierCode;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.infrastructure.rest.dto.request.InternalCreateShipmentRequest;
import com.fulfillment.shippingservice.infrastructure.rest.dto.response.ShipmentResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal/v1/shipments")
@Tag(name = "Internal - Shipping", description = "Endpoints internos para comunicación entre servicios")
public class InternalShippingController {

    private final ShippingService shippingService;

    public InternalShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse createShipment(@Valid @RequestBody InternalCreateShipmentRequest request) {
        CreateShipmentCommand command = new CreateShipmentCommand(
                request.orderId(),
                request.warehouseId(),
                CarrierCode.INTERNAL_CARRIER,
                request.items().stream()
                        .map(i -> new CreateShipmentCommand.Item(i.sku(), i.quantity()))
                        .toList(),
                Instant.now().plus(7, ChronoUnit.DAYS));

        Shipment shipment = shippingService.create(command);
        return ShipmentRestMapper.toResponse(shipment);
    }
}
