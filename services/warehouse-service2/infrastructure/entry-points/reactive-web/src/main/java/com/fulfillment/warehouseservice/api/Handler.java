package com.fulfillment.warehouseservice.api;

import com.fulfillment.warehouseservice.api.dto.CreateWarehouseRequest;
import com.fulfillment.warehouseservice.api.dto.WarehouseResponse;
import com.fulfillment.warehouseservice.usecase.operation.WarehouseUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class Handler {

    private final WarehouseUseCase warehouseUseCase;

    public Handler(WarehouseUseCase warehouseUseCase) {
        this.warehouseUseCase = warehouseUseCase;
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(CreateWarehouseRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("request body is required")))
                .flatMap(body -> warehouseUseCase.create(body.city(), body.lat(), body.lng()))
                .map(WarehouseResponse::from)
                .flatMap(response -> ServerResponse.status(HttpStatus.CREATED).bodyValue(response));
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        String warehouseId = request.pathVariable("id");
        return warehouseUseCase.getById(warehouseId)
                .map(WarehouseResponse::from)
                .flatMap(ServerResponse.ok()::bodyValue);
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        String warehouseId = request.pathVariable("id");
        return warehouseUseCase.delete(warehouseId)
                .then(ServerResponse.noContent().build());
    }
}
