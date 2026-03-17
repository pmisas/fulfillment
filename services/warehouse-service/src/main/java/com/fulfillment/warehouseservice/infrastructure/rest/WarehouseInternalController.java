package com.fulfillment.warehouseservice.infrastructure.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.warehouseservice.application.WarehouseService;
import com.fulfillment.warehouseservice.infrastructure.rest.dto.WarehouseResponse;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/internal/v1/warehouses")
@Tag(name = "Internal - Warehouse", description = "Endpoints internos para comunicación entre servicios")
public class WarehouseInternalController {

    private final WarehouseService warehouseService;
    
    public WarehouseInternalController(WarehouseService warehouseService) {
      this.warehouseService = warehouseService;
    }

 
    @RequestMapping(path ="/{id}", method = RequestMethod.HEAD)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> existsById(@PathVariable String id) {
        if (warehouseService.existsById(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseService.getAll().stream()
                .map(WarehouseRestMapper::toResponse)
                .toList();
    }

}