package com.fulfillment.warehouseservice.infrastructure.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.warehouseservice.application.WarehouseService;
import com.fulfillment.warehouseservice.application.dto.CreateWarehouseCommand;
import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.infrastructure.rest.dto.CreateWarehouseRequest;
import com.fulfillment.warehouseservice.infrastructure.rest.dto.WarehouseResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/warehouses")
public class WarehouseController {
    
    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse createWarehouses(
            @Valid @RequestBody CreateWarehouseRequest req) {
        CreateWarehouseCommand command = WarehouseRestMapper.toCommand(req);
        Warehouse warehouse = warehouseService.create(command);
        return WarehouseRestMapper.toResponse(warehouse);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public WarehouseResponse getWarehouseById(@PathVariable("id") String id) {
        Warehouse warehouse = warehouseService.getById(id);
        return WarehouseRestMapper.toResponse(warehouse);
    }

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseService.getAll().stream()
                .map(WarehouseRestMapper::toResponse)
                .toList();
    }

}
