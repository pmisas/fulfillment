package com.fulfillment.shippingservice.infrastructure.rest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fulfillment.shippingservice.application.ShippingService;
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
@Tag(name = "Shipments", description = "Gestión de envíos y guías de despacho")
@SecurityRequirement(name = "bearerAuth")
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @Operation(summary = "Consultar envío por ID", description = "Retorna el detalle de un envío.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Envío encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShipmentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Envío no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ShipmentResponse getById(
            @Parameter(description = "ID del envío", required = true, example = "ship-abc123")
            @PathVariable("id") String shipmentId) {
        Shipment shipment = shippingService.getById(shipmentId);
        return ShipmentRestMapper.toResponse(shipment);
    }

    @Operation(summary = "Listar envíos", description = "Retorna todos los envíos, opcionalmente filtrados por orderId.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de envíos",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ShipmentResponse.class)))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ShipmentResponse> getShipments(
            @Parameter(description = "ID de la orden para filtrar envíos", example = "8d91c9aa-1234-4567-890a-abcdef123456")
            @RequestParam(value = "orderId", required = false) String orderId) {
        List<Shipment> shipments = (orderId == null || orderId.isBlank())
                ? shippingService.getAll()
                : shippingService.getByOrderId(orderId);

        return shipments.stream().map(ShipmentRestMapper::toResponse).toList();
    }

    @Operation(summary = "Marcar envío como despachado", description = "Cambia el estado del envío a SHIPPED. Requiere rol WAREHOUSE_MANAGER o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Envío marcado como despachado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShipmentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Transición de estado inválida",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Envío no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{id}/ship")
    @ResponseStatus(HttpStatus.OK)
    public ShipmentResponse markAsShipped(
            @Parameter(description = "ID del envío", required = true, example = "ship-abc123")
            @PathVariable("id") String shipmentId) {
        Shipment shipment = shippingService.markAsShipped(shipmentId);
        return ShipmentRestMapper.toResponse(shipment);
    }

    @Operation(summary = "Marcar envío como entregado", description = "Cambia el estado del envío a DELIVERED. Requiere rol WAREHOUSE_MANAGER o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Envío marcado como entregado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShipmentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Transición de estado inválida",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Envío no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{id}/deliver")
    @ResponseStatus(HttpStatus.OK)
    public ShipmentResponse markAsDelivered(
            @Parameter(description = "ID del envío", required = true, example = "ship-abc123")
            @PathVariable("id") String shipmentId) {
        Shipment shipment = shippingService.markAsDelivered(shipmentId);
        return ShipmentRestMapper.toResponse(shipment);
    }

    @Operation(summary = "Obtener URL de guía de despacho", description = "Retorna una URL pre-firmada de S3 con la guía de despacho. El enlace expira en 15 minutos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL de la guía obtenida correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShippingGuideUrlResponse.class))),
        @ApiResponse(responseCode = "404", description = "Envío no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "La guía de despacho aún no está lista",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}/guide")
    @ResponseStatus(HttpStatus.OK)
    public ShippingGuideUrlResponse getShippingGuide(
            @Parameter(description = "ID del envío", required = true, example = "ship-abc123")
            @PathVariable("id") String shipmentId) {
        String url = shippingService.getShippingGuideUrl(shipmentId);
        return new ShippingGuideUrlResponse(url, Instant.now().plus(15, ChronoUnit.MINUTES));
    }

}

