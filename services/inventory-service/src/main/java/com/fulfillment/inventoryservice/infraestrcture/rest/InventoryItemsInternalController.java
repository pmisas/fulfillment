package com.fulfillment.inventoryservice.infraestrcture.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.inventoryservice.application.InventoryItemsService;
import com.fulfillment.inventoryservice.application.dto.AvailabilityQuery;
import com.fulfillment.inventoryservice.application.dto.AvailabilityResult;
import com.fulfillment.inventoryservice.application.dto.ConsumeReservationCommand;
import com.fulfillment.inventoryservice.application.dto.ReserveBatchCommand;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction.ConsumeResult;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction.ReserveResult;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.BatchRequest;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.CheckAvailabilityResponse;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.ReserveItemsRequest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal/v1")
@Tag(name = "Internal - Inventory", description = "Endpoints internos para comunicación entre servicios")
public class InventoryItemsInternalController {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryItemsInternalController.class);
    private final InventoryItemsService inventoryService;

    public InventoryItemsInternalController(InventoryItemsService inventoryService) {
        this.inventoryService = inventoryService;
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
        log.info("DELETE /internal/v1/reservations/{} - Starting release", reservationId);
        try {
            inventoryService.releaseReservation(reservationId);
            log.info("DELETE /internal/v1/reservations/{} - Completed successfully", reservationId);
        } catch (Exception e) {
            log.error("DELETE /internal/v1/reservations/{} - FAILED: {}", reservationId, e.getMessage(), e);
            throw e;
        }
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
