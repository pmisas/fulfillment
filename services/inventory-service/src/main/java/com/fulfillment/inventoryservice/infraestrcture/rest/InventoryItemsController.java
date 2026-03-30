package com.fulfillment.inventoryservice.infraestrcture.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.inventoryservice.application.InventoryWarehouseAuthorizationService;
import com.fulfillment.inventoryservice.application.InventoryItemsService;
import com.fulfillment.inventoryservice.application.dto.RestockBatchCommand;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.ApiErrorResponse;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.BatchRequest;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.InventoryItemResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Inventory", description = "Gestión del inventario por bodega")
@SecurityRequirement(name = "bearerAuth")
public class InventoryItemsController {

    private final InventoryItemsService inventoryService;
    private final InventoryWarehouseAuthorizationService inventoryWarehouseAuthorizationService;

    public InventoryItemsController(
            InventoryItemsService inventoryService,
            InventoryWarehouseAuthorizationService inventoryWarehouseAuthorizationService) {
        this.inventoryService = inventoryService;
        this.inventoryWarehouseAuthorizationService = inventoryWarehouseAuthorizationService;
    }

    @Operation(
        summary = "Reabastecer inventario",
        description = "Agrega stock a los ítems indicados en la bodega. Requiere rol WAREHOUSE_MANAGER o ADMIN."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventario actualizado correctamente",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = InventoryItemResponse.class)))),
        @ApiResponse(responseCode = "400", description = "Request con campos inválidos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene permisos sobre la bodega",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Bodega no encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/warehouses/{warehouseId}/inventory/restock")
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryItemResponse> restockBatch(
            @Parameter(description = "ID de la bodega", required = true, example = "warehouse-001")
            @PathVariable String warehouseId,
            @Valid @RequestBody BatchRequest req,
            Authentication authentication) {

        inventoryWarehouseAuthorizationService.assertCanAccessWarehouse(authentication, warehouseId);

        RestockBatchCommand command = InventoryRestMapper.toRestockBatchCommand(warehouseId, req);
        return inventoryService.restockBatch(command).stream()
                .map(InventoryRestMapper::toResponse)
                .toList();
    }

    @Operation(
        summary = "Consultar inventario de una bodega",
        description = "Retorna todos los ítems de inventario de la bodega indicada. Requiere rol WAREHOUSE_MANAGER o ADMIN."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventario obtenido correctamente",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = InventoryItemResponse.class)))),
        @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene permisos sobre la bodega",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Bodega no encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/warehouses/{warehouseId}/inventory")
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryItemResponse> getInventoryByWarehouse(
            @Parameter(description = "ID de la bodega", required = true, example = "warehouse-001")
            @PathVariable String warehouseId,
            Authentication authentication) {
        inventoryWarehouseAuthorizationService.assertCanAccessWarehouse(authentication, warehouseId);
        return inventoryService.getByWarehouseId(warehouseId).stream()
                .map(InventoryRestMapper::toResponse)
                .toList();
    }

}
