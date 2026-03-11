# order-service

Microservicio encargado de la **creación, consulta y cancelación de órdenes** dentro de la plataforma de fulfillment.

## Responsabilidad

Este servicio maneja el ciclo inicial de la orden:

- crea nuevas órdenes,
- consulta órdenes por id,
- recibe solicitudes de cancelación,
- registra el estado inicial de la orden,
- publica eventos de dominio para que otros componentes continúen el flujo.

## Funcionalidades principales

- Creación de órdenes con `Idempotency-Key`
- Consulta de órdenes
- Cancelación de órdenes
- Persistencia de historial de estado
- Publicación de eventos mediante **Outbox Pattern**

## Estados manejados

La orden se crea inicialmente en:

- `RECEIVED`

Y desde este servicio también se puede solicitar cancelación, generando el evento correspondiente para procesamiento asíncrono.

## Eventos publicados

- `OrderReceived`
- `OrderCancelled`

## Endpoints principales

### Crear orden
```http
POST /api/v1/orders
```
Headers:
```http
Idempotency-Key: <unique-key>
```

Body:
```json
{
  "lat": 4.7110,
  "lng": -74.0721,
  "items": [
    { "sku": "SKU-1", "quantity": 2 },
    { "sku": "SKU-2", "quantity": 1 }
  ]
}
```

### Consultar orden
```http
GET /api/v1/orders/{orderId}
```

## Componentes técnicos principales

- Spring Boot
- DynamoDB para persistencia de órdenes, historial y outbox
- Redis para idempotencia
- Spring Security + JWT/Cognito
- Outbox Pattern para publicación confiable de eventos

## Persistencia

Este servicio trabaja con:
- tabla de órdenes
- tabla de historial de estados
- tabla outbox
- Redis para control de idempotencia

## Seguridad

Los endpoints /api/v1/orders/** requieren autenticación y roles autorizados.

Roles observados en este servicio:
- OPERATOR
- ADMIN
