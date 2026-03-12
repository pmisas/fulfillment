package com.fulfillment.warehouseservice.infrastructure.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.warehouseservice.application.WarehouseService;
import com.fulfillment.warehouseservice.application.dto.CreateWarehouseCommand;
import com.fulfillment.warehouseservice.application.dto.WarehouseStartFlowCommand;
import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.infrastructure.rest.dto.ApiErrorResponse;
import com.fulfillment.warehouseservice.infrastructure.rest.dto.CreateWarehouseRequest;
import com.fulfillment.warehouseservice.infrastructure.rest.dto.WarehouseResponse;

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


@RestController
@RequestMapping("/api/v1/warehouses")
@Tag(name = "Warehouses", description = "Gestión de bodegas y flujo de picking/packing")
@SecurityRequirement(name = "bearerAuth")
public class WarehouseController {
    
    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @Operation(summary = "Crear bodega", description = "Crea una nueva bodega. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Bodega creada correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WarehouseResponse.class))),
        @ApiResponse(responseCode = "400", description = "Request con campos inválidos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest req) {
        CreateWarehouseCommand command = WarehouseRestMapper.toCommand(req);
        Warehouse warehouse = warehouseService.create(command);
        return WarehouseRestMapper.toResponse(warehouse);
    }

    @Operation(summary = "Consultar bodega por ID", description = "Retorna los datos de una bodega. Requiere rol WAREHOUSE_MANAGER, OPERATOR o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bodega encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WarehouseResponse.class))),
        @ApiResponse(responseCode = "404", description = "Bodega no encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public WarehouseResponse getWarehouseById(
            @Parameter(description = "ID de la bodega", required = true, example = "warehouse-001")
            @PathVariable("id") String id) {
        Warehouse warehouse = warehouseService.getById(id);
        return WarehouseRestMapper.toResponse(warehouse);
    }

    @Operation(summary = "Listar todas las bodegas", description = "Retorna todas las bodegas registradas. Requiere rol WAREHOUSE_MANAGER, OPERATOR o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de bodegas",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = WarehouseResponse.class)))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseService.getAll().stream()
                .map(WarehouseRestMapper::toResponse)
                .toList();
    }

    @Operation(summary = "Completar picking", description = "Registra que el picking de una orden en la bodega fue completado. Requiere rol WAREHOUSE_MANAGER o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Picking completado, procesando"),
        @ApiResponse(responseCode = "404", description = "Bodega u orden no encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{warehouseId}/orders/{orderId}/picking/complete")
    public ResponseEntity<Void> completePicking(
                @Parameter(description = "ID de la bodega", required = true, example = "warehouse-001")
                @PathVariable String warehouseId,
                @Parameter(description = "ID de la orden", required = true, example = "8d91c9aa-1234-4567-890a-abcdef123456")
                @PathVariable String orderId) {

        WarehouseStartFlowCommand command = new WarehouseStartFlowCommand(warehouseId, orderId);
        warehouseService.completePicking(command);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Completar packing", description = "Registra que el packing de una orden en la bodega fue completado. Requiere rol WAREHOUSE_MANAGER o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Packing completado, procesando"),
        @ApiResponse(responseCode = "404", description = "Bodega u orden no encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{warehouseId}/orders/{orderId}/packing/complete")
    public ResponseEntity<Void> completePacking(
                @Parameter(description = "ID de la bodega", required = true, example = "warehouse-001")
                @PathVariable String warehouseId,
                @Parameter(description = "ID de la orden", required = true, example = "8d91c9aa-1234-4567-890a-abcdef123456")
                @PathVariable String orderId) {

        WarehouseStartFlowCommand command = new WarehouseStartFlowCommand(warehouseId, orderId);
        warehouseService.completePacking(command);
        return ResponseEntity.accepted().build();
    }

}

