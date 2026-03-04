package com.fulfillment.inventoryservice.infraestrcture.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.inventoryservice.application.InventoryItemsService;
import com.fulfillment.inventoryservice.application.dto.RestockBatchCommand;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.BatchRequest;
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


    @GetMapping("/warehouses/{warehouseId}/inventory") //p
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryItemResponse> getInventoryByWarehouse(@PathVariable String warehouseId) {
        return inventoryService.getByWarehouseId(warehouseId).stream()
                .map(InventoryRestMapper::toResponse)
                .toList();
    }

}