package com.fulfillment.inventoryservice.infraestrcture.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.inventoryservice.application.InventoryItemsService;
import com.fulfillment.inventoryservice.application.dto.AvailabilityQuery;
import com.fulfillment.inventoryservice.application.dto.AvailabilityResult;
import com.fulfillment.inventoryservice.application.dto.ConsumeReservationCommand;
import com.fulfillment.inventoryservice.application.dto.ReserveBatchCommand;
import com.fulfillment.inventoryservice.application.dto.RestockBatchCommand;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction.ConsumeResult;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction.ReserveResult;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.BatchRequest;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.CheckAvailabilityResponse;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.InventoryItemResponse;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.ReserveItemsRequest;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1")
public class InventoryItemsController {

    private final InventoryItemsService inventoryService;

    public InventoryItemsController(InventoryItemsService inventoryService) {
        this.inventoryService = inventoryService;
    }


    @PostMapping("/warehouses/{warehouseId}/inventory/restock")
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryItemResponse> restockBatch(
            @PathVariable String warehouseId,
            @Valid @RequestBody BatchRequest req) {

        RestockBatchCommand command = InventoryRestMapper.toRestockBatchCommand(warehouseId, req);
        return inventoryService.restockBatch(command).stream()
                .map(InventoryRestMapper::toResponse)
                .toList();
    }

    @PostMapping("/warehouses/{warehouseId}/inventory/availability")
    @ResponseStatus(HttpStatus.OK)
    public CheckAvailabilityResponse checkAvailability(
            @PathVariable String warehouseId,
            @Valid @RequestBody BatchRequest req) {

        AvailabilityQuery query = InventoryRestMapper.toAvailabilityQuery(warehouseId, req);
        AvailabilityResult result = inventoryService.checkAvailability(query);
        return InventoryRestMapper.toAvailabilityResponse(result);
    }


    @GetMapping("/warehouses/{warehouseId}/inventory")
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryItemResponse> getInventoryByWarehouse(@PathVariable String warehouseId) {
        return inventoryService.getByWarehouseId(warehouseId).stream()
                .map(InventoryRestMapper::toResponse)
                .toList();
    }


    @PostMapping("/warehouses/{warehouseId}/reservations")
    public ResponseEntity<Void> reserveItems(
            @PathVariable String warehouseId,
            @Valid @RequestBody ReserveItemsRequest req) {

        ReserveBatchCommand command = InventoryRestMapper.toReserveBatchCommand(warehouseId, req);
        ReserveResult result = inventoryService.reserveItems(command);

        return switch (result) {
            case RESERVED           -> ResponseEntity.status(HttpStatus.CREATED).build();
            case ALREADY_RESERVED   -> ResponseEntity.ok().build();
            case INSUFFICIENT_STOCK -> ResponseEntity.unprocessableEntity().build();
        };
    }

    @DeleteMapping("/reservations/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseReservation(@PathVariable String reservationId) {
        inventoryService.releaseReservation(reservationId);
    }

    @PostMapping("/reservations/{reservationId}/consume")
    public ResponseEntity<Void> consumeReservation(@PathVariable String reservationId) {

        ConsumeResult result = inventoryService.consumeReservation(
                new ConsumeReservationCommand(reservationId)
        );

        return switch (result) {
            case CONSUMED -> ResponseEntity.noContent().build();
            case RESERVATION_NOT_FOUND -> ResponseEntity.notFound().build();
        };
    }
}