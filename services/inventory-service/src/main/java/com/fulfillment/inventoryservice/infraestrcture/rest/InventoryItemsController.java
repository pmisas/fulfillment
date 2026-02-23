package com.fulfillment.inventoryservice.infraestrcture.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.inventoryservice.application.InventoryItemsService;
import com.fulfillment.inventoryservice.application.dto.InventoryCommand;
import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.AmountRequest;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.InventoryItemResponse;

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

    @PostMapping("warehouses/{warehouseId}/inventory/{sku}/consume")
    @ResponseStatus(HttpStatus.OK)
    public InventoryItemResponse consumeInventory(
            @PathVariable String warehouseId,
            @PathVariable String sku,
            @Valid @RequestBody AmountRequest req) {
         
        InventoryCommand command = InventoryRestMapper.toCommand(warehouseId, sku, req.amount());
        InventoryItem item =inventoryService.consume(command);

        return InventoryRestMapper.toResponse(item);
    }
    
    @PostMapping("/warehouses/{warehouseId}/inventory/{sku}/restock")
    @ResponseStatus(HttpStatus.OK)
    public InventoryItemResponse restockInventory(
            @PathVariable String warehouseId,
            @PathVariable String sku,
            @Valid @RequestBody AmountRequest req) {
        
        InventoryCommand command = InventoryRestMapper.toCommand(warehouseId, sku, req.amount());
        InventoryItem item =inventoryService.restock(command);

        return InventoryRestMapper.toResponse(item);
    }

    @PostMapping("/warehouses/{warehouseId}/inventory/{sku}/reserve")
    @ResponseStatus(HttpStatus.OK)
    public InventoryItemResponse reserveInventory(
        @PathVariable String warehouseId,
        @PathVariable String sku,
        @Valid @RequestBody AmountRequest req) {

        InventoryCommand command = InventoryRestMapper.toCommand(warehouseId, sku, req.amount());
        InventoryItem item = inventoryService.reserve(command);

        return InventoryRestMapper.toResponse(item);
    }

    @PostMapping("/warehouses/{warehouseId}/inventory/{sku}/release")
    @ResponseStatus(HttpStatus.OK)
    public InventoryItemResponse releaseInventory(
        @PathVariable String warehouseId,
        @PathVariable String sku,
        @Valid @RequestBody AmountRequest req
    ) {
        InventoryCommand command = InventoryRestMapper.toCommand(warehouseId, sku, req.amount());
        InventoryItem item = inventoryService.release(command);

        return InventoryRestMapper.toResponse(item);
    }

    @GetMapping("/warehouse/{warehouseId}/inventory")
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryItemResponse> getInventoryByWarehouse(@PathVariable String warehouseId) {
        
        return inventoryService.getByWarehouseId(warehouseId).stream()
        .map(InventoryRestMapper::toResponse)
        .toList();
    }

}
