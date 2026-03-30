package com.fulfillment.shippingservice.infrastructure.rest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.shippingservice.application.ShippingService;
import com.fulfillment.shippingservice.application.ShippingWarehouseAuthorizationService;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.infrastructure.rest.dto.ApiErrorResponse;
import com.fulfillment.shippingservice.infrastructure.rest.dto.ShipmentResponse;
import com.fulfillment.shippingservice.infrastructure.rest.dto.ShippingGuideUrlResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/shipments")
@Tag(name = "Shipments", description = "Gestion de envios y guias de despacho")
@SecurityRequirement(name = "bearerAuth")
public class ShippingController {

    private final ShippingService shippingService;
    private final ShippingWarehouseAuthorizationService shippingWarehouseAuthorizationService;

    public ShippingController(
            ShippingService shippingService,
            ShippingWarehouseAuthorizationService shippingWarehouseAuthorizationService) {
        this.shippingService = shippingService;
        this.shippingWarehouseAuthorizationService = shippingWarehouseAuthorizationService;
    }

    @Operation(summary = "Consultar envio por ID", description = "Retorna el detalle de un envio.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Envio encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShipmentResponse.class))),
        @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene permisos sobre este envío",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Envio no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ShipmentResponse getById(
            @Parameter(description = "ID del envio", required = true, example = "ship-abc123")
            @PathVariable("id") String shipmentId,
            Authentication authentication) {
        Shipment shipment = shippingService.getById(shipmentId);
        shippingWarehouseAuthorizationService.assertCanAccessShipment(authentication, shipment);
        return ShipmentRestMapper.toResponse(shipment);
    }

    @Operation(summary = "Listar envios", description = "ADMIN ve todos los envios. WAREHOUSE_MANAGER solo los de su bodega asignada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de envios",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ShipmentResponse.class)))),
        @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene permisos para consultar estos envíos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ShipmentResponse> getShipments(
            @Parameter(description = "ID de la orden para filtrar envios", example = "8d91c9aa-1234-4567-890a-abcdef123456")
            @RequestParam(value = "orderId", required = false) String orderId,
            Authentication authentication) {
        List<Shipment> shipments = (orderId == null || orderId.isBlank())
                ? shippingService.getAll()
                : shippingService.getByOrderId(orderId);

        return shippingWarehouseAuthorizationService.filterAuthorizedShipments(authentication, shipments)
                .stream()
                .map(ShipmentRestMapper::toResponse)
                .toList();
    }

    @Operation(summary = "Marcar envio como despachado", description = "ADMIN puede operar cualquier envio. WAREHOUSE_MANAGER solo los de su bodega asignada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Envio marcado como despachado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShipmentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Transicion de estado invalida",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene permisos sobre este envío",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Envio no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{id}/ship")
    @ResponseStatus(HttpStatus.OK)
    public ShipmentResponse markAsShipped(
            @Parameter(description = "ID del envio", required = true, example = "ship-abc123")
            @PathVariable("id") String shipmentId,
            Authentication authentication) {
        Shipment current = shippingService.getById(shipmentId);
        shippingWarehouseAuthorizationService.assertCanAccessShipment(authentication, current);
        Shipment shipment = shippingService.markAsShipped(shipmentId);
        return ShipmentRestMapper.toResponse(shipment);
    }

    @Operation(summary = "Marcar envio como entregado", description = "ADMIN puede operar cualquier envio. WAREHOUSE_MANAGER solo los de su bodega asignada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Envio marcado como entregado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShipmentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Transicion de estado invalida",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene permisos sobre este envío",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Envio no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{id}/deliver")
    @ResponseStatus(HttpStatus.OK)
    public ShipmentResponse markAsDelivered(
            @Parameter(description = "ID del envio", required = true, example = "ship-abc123")
            @PathVariable("id") String shipmentId,
            Authentication authentication) {
        Shipment current = shippingService.getById(shipmentId);
        shippingWarehouseAuthorizationService.assertCanAccessShipment(authentication, current);
        Shipment shipment = shippingService.markAsDelivered(shipmentId);
        return ShipmentRestMapper.toResponse(shipment);
    }

    @Operation(summary = "Obtener URL de guia de despacho", description = "ADMIN puede consultar cualquier guia. WAREHOUSE_MANAGER solo las de su bodega asignada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL de la guia obtenida correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShippingGuideUrlResponse.class))),
        @ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene permisos sobre este envío",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Envio no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "La guia de despacho aun no esta lista",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}/guide")
    @ResponseStatus(HttpStatus.OK)
    public ShippingGuideUrlResponse getShippingGuide(
            @Parameter(description = "ID del envio", required = true, example = "ship-abc123")
            @PathVariable("id") String shipmentId,
            Authentication authentication) {
        Shipment shipment = shippingService.getById(shipmentId);
        shippingWarehouseAuthorizationService.assertCanAccessShipment(authentication, shipment);
        String url = shippingService.getShippingGuideUrl(shipmentId);
        return new ShippingGuideUrlResponse(url, Instant.now().plus(15, ChronoUnit.MINUTES));
    }
}
