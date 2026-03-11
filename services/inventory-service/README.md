# inventory-service

Microservicio encargado de la **gestión de inventario por bodega y SKU** dentro de la plataforma de fulfillment.

## Responsabilidad

Este servicio administra el stock disponible en cada bodega y expone operaciones internas para soportar el flujo de órdenes.

Sus funciones principales son:

- reabastecer inventario,
- consultar inventario por bodega,
- validar disponibilidad de productos,
- reservar stock para una orden,
- consumir una reserva cuando la orden avanza,
- liberar una reserva cuando una orden se cancela.

## Funcionalidades principales

- Restock de inventario por lote
- Consulta de inventario por bodega
- Validación de disponibilidad
- Reserva de stock
- Consumo de reservas
- Liberación de reservas

## Endpoints principales

### Reabastecer inventario
```http
POST /api/v1/warehouses/{warehouseId}/inventory/restock
```

Body:
```json
{
  "items": [
    { "sku": "SKU-1", "quantity": 50 },
    { "sku": "SKU-2", "quantity": 25 }
  ]
}
```

## Consultar inventario por bodega
```http
GET /api/v1/warehouses/{warehouseId}/inventory
```

## Consultar disponibilidad
```http
POST /internal/v1/warehouses/{warehouseId}/inventory/availability
```

## Reservar inventario
```http
POST /internal/v1/warehouses/{warehouseId}/reservations
```

## Liberar reserva
```http
DELETE /internal/v1/reservations/{reservationId}
```

## Consumir reserva
```http
POST /internal/v1/reservations/{reservationId}/consume
```

## Componentes técnicos principales

- Spring Boot
- DynamoDB para inventario y reservas
- Transacciones DynamoDB para restock, reserva, liberación y consumo
- Spring Security + JWT/Cognito
- Cliente HTTP interno hacia warehouse-service

## Persistencia

Este servicio trabaja con:
- tabla de inventario
- tabla de reservas de inventario

## Seguridad

Los endpoints públicos requieren autenticación y autorización según rol.

Roles observados en este servicio:
- WAREHOUSE_MANAGER
- ADMIN

Los endpoints internos se usan para comunicación entre microservicios.