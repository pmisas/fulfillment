package com.fulfillment.warehouseservice.infrastructure.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.warehouseservice.application.WarehouseAccessService;
import com.fulfillment.warehouseservice.domain.model.WarehouseAccess;
import com.fulfillment.warehouseservice.infrastructure.rest.dto.response.ApiErrorResponse;
import com.fulfillment.warehouseservice.infrastructure.rest.dto.response.UserWarehouseAccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Warehouse Access", description = "Consulta de asignaciones de acceso a bodegas")
@SecurityRequirement(name = "bearerAuth")
public class UserWarehouseAccessController {

    private final WarehouseAccessService warehouseAccessService;

    public UserWarehouseAccessController(WarehouseAccessService warehouseAccessService) {
        this.warehouseAccessService = warehouseAccessService;
    }

    @Operation(summary = "Consultar acceso de warehouse por usuario", description = "Retorna la asignacion almacenada para un usuario. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Acceso encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserWarehouseAccessResponse.class))),
        @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene permisos para consultar accesos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Acceso no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{userId}/warehouse-access")
    public UserWarehouseAccessResponse getWarehouseAccessByUser(@PathVariable String userId) {
        WarehouseAccess access = warehouseAccessService.getWarehouseAccessByUser(userId);
        return new UserWarehouseAccessResponse(
            access.getUserId(),
            access.getWarehouseId(),
            access.isActive(),
            access.getAssignedAt(),
            access.getAssignedBy(),
            access.getUpdatedAt()
        );
    }
}
