# warehouse-service

Microservicio encargado de la **gestión de bodegas** y de la publicación de eventos operativos del flujo logístico.

## Responsabilidad

Este servicio administra la información de las bodegas y expone operaciones relacionadas con el proceso interno de preparación de órdenes.

Sus funciones principales son:

- crear bodegas,
- consultar bodegas,
- validar si una bodega existe,
- publicar eventos cuando una orden completa picking o packing.

## Funcionalidades principales

- Registro de nuevas bodegas
- Consulta de bodegas por id
- Listado de bodegas
- Endpoint interno de validación de existencia
- Publicación de eventos operativos mediante **Outbox Pattern**

## Eventos publicados

- `PickingCompleted`
- `PackingCompleted`

## Endpoints principales

### Crear bodega
```http
POST /api/v1/warehouses
```

Body:
```json
{
  "city": "Bogota",
  "lat": 4.7110,
  "lng": -74.0721
}
```

### Consultar bodega por id
```http
Consultar bodega por id
```

### Listar bodegas
```http
GET /api/v1/warehouses
```

### Marcar picking completado
```http
POST /api/v1/warehouses/{warehouseId}/orders/{orderId}/picking/complete
```

### Marcar packing completado
```http
POST /api/v1/warehouses/{warehouseId}/orders/{orderId}/packing/complete
```

### Validar existencia de bodega
```http
HEAD /internal/v1/warehouses/{warehouseId}
```

### Listado interno de bodegas
```http
GET /internal/v1/warehouses
```

## Componentes técnicos principales

- Spring Boot
- DynamoDB para persistencia de bodegas y outbox
- Spring Security + JWT/Cognito
- Outbox Pattern para eventos operativos

## Persistencia

Este servicio trabaja con:
- tabla de bodegas
- tabla outbox

## Seguridad

Los endpoints públicos requieren autenticación y autorización según rol.

Roles en este servicio:
- ADMIN
- WAREHOUSE_MANAGER
- OPERATOR

Los endpoints internos se usan para comunicación entre microservicios.
