# shipping-service

Microservicio encargado de la **creación y gestión de envíos** dentro de la plataforma de fulfillment.

## Responsabilidad

Este servicio administra el ciclo de vida del shipment asociado a una orden ya preparada.

Sus funciones principales son:

- crear shipments,
- consultar envíos,
- marcar envíos como despachados o entregados,
- generar guías de envío en PDF,
- almacenar guías en S3,
- publicar eventos cuando un envío es despachado.

## Funcionalidades principales

- Creación de shipments
- Consulta de shipments por id o por orden
- Cambio de estado del shipment
- Generación de guía PDF
- Almacenamiento de guía en S3
- Exposición de URL prefirmada para descarga
- Publicación de eventos mediante **Outbox Pattern**

## Estados manejados

- `PENDING`
- `SHIPPED`
- `DELIVERED`

## Eventos publicados

- `ShipmentShipped`

## Endpoints principales

### Crear shipment
```http
POST /internal/v1/shipments
```

Body:
```json
{
  "orderId": "order-123",
  "warehouseId": "warehouse-1",
  "items": [
    { "sku": "SKU-1", "quantity": 2 },
    { "sku": "SKU-2", "quantity": 1 }
  ]
}
```

### Consultar shipment por id
```http
GET /api/v1/shipments/{shipmentId}
```

### Listar shipments
```http
GET /api/v1/shipments
```

### Listar shipments por orderId
```http
GET /api/v1/shipments?orderId={orderId}
```

### Marcar shipment como enviado
```http
POST /api/v1/shipments/{shipmentId}/ship
```

### Marcar shipment como entregado
```http
POST /api/v1/shipments/{shipmentId}/deliver
```

### Obtener URL de guía
```http
GET /api/v1/shipments/{shipmentId}/guide
```

## Componentes técnicos principales

- Spring Boot
- DynamoDB para persistencia de shipments y outbox
- Amazon S3 para almacenamiento de guías
- Apache PDFBox para generación de PDF
- Spring Security + JWT/Cognito
- Outbox Pattern para publicación de eventos

## Persistencia

Este servicio trabaja con:

- tabla de shipments
- tabla outbox
- bucket S3 para guías de envío

## Seguridad

Los endpoints públicos requieren autenticación y autorización según rol.

Roles en este servicio:
- WAREHOUSE_MANAGER
- OPERATOR
- ADMIN

Los endpoints internos se usan para comunicación entre microservicios.