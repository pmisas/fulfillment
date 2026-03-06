package com.fulfillment.shippingservice.infrastructure.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.shippingservice.application.ShippingService;
import com.fulfillment.shippingservice.application.dto.CreateShipmentCommand;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.infrastructure.rest.dto.CreateShipmentRequest;
import com.fulfillment.shippingservice.infrastructure.rest.dto.MarkShippedRequest;
import com.fulfillment.shippingservice.infrastructure.rest.dto.ShipmentResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        CreateShipmentCommand command = ShipmentRestMapper.toCommand(request);
        Shipment shipment = shippingService.create(command);
        return ShipmentRestMapper.toResponse(shipment);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ShipmentResponse getById(@PathVariable("id") String shipmentId) {
        Shipment shipment = shippingService.getById(shipmentId);
        return ShipmentRestMapper.toResponse(shipment);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ShipmentResponse> getShipments(
            @RequestParam(value = "orderId", required = false) String orderId) {
        List<Shipment> shipments = (orderId == null || orderId.isBlank())
                ? shippingService.getAll()
                : shippingService.getByOrderId(orderId);

        return shipments.stream().map(ShipmentRestMapper::toResponse).toList();
    }

    @PostMapping("/{id}/ship")
    @ResponseStatus(HttpStatus.OK)
    public ShipmentResponse markAsShipped(
            @PathVariable("id") String shipmentId,
            @Valid @RequestBody MarkShippedRequest request) {
        Shipment shipment = shippingService.markAsShipped(shipmentId, request.trackingId());
        return ShipmentRestMapper.toResponse(shipment);
    }

    @PostMapping("/{id}/in-transit")
    @ResponseStatus(HttpStatus.OK)
    public ShipmentResponse markInTransit(@PathVariable("id") String shipmentId) {
        Shipment shipment = shippingService.markInTransit(shipmentId);
        return ShipmentRestMapper.toResponse(shipment);
    }

    @PostMapping("/{id}/deliver")
    @ResponseStatus(HttpStatus.OK)
    public ShipmentResponse markAsDelivered(@PathVariable("id") String shipmentId) {
        Shipment shipment = shippingService.markAsDelivered(shipmentId);
        return ShipmentRestMapper.toResponse(shipment);
    }

}
