# Fulfillment Logistics Platform

Plataforma distribuida de logística y fulfillment orientada al procesamiento de órdenes, validación de inventario, asignación de bodegas, preparación en almacén, generación de envíos y publicación de eventos mediante microservicios y arquitectura orientada a eventos.

---

## Tabla de contenido

- [Descripción general](#descripción-general)
- [Objetivo del proyecto](#objetivo-del-proyecto)
- [Microservicios](#microservicios)
- [Flujo funcional end-to-end](#flujo-funcional-end-to-end)
- [Modelo de estados de la orden](#modelo-de-estados-de-la-orden)
- [Patrones de arquitectura usados](#patrones-de-arquitectura-usados)
- [Tecnologías utilizadas](#tecnologías-utilizadas)
- [Seguridad](#seguridad)
- [Persistencia y mensajería](#persistencia-y-mensajería)
- [APIs principales](#apis-principales)
- [Ejecución y despliegue](#ejecución-y-despliegue)
- [Idempotencia y concurrencia](#idempotencia-y-concurrencia)
- [Mejoras futuras](#mejoras-futuras)

---

## Descripción general

Este proyecto implementa una plataforma de fulfillment para procesar órdenes logísticas de manera distribuida. El sistema permite:

- recibir órdenes de compra,
- validar disponibilidad de inventario,
- seleccionar la mejor bodega candidata,
- reservar stock,
- avanzar el flujo operativo de picking y packing,
- generar envíos y guías de despacho,
- actualizar el estado global de la orden a través de eventos asíncronos.

La solución fue diseñada con enfoque de **microservicios**, integraciones HTTP internas, mensajería por **Amazon SQS**, persistencia en **DynamoDB**, soporte de idempotencia con **Redis**, publicación de eventos mediante **Outbox Pattern** y almacenamiento de documentos en **Amazon S3**.

---

## Objetivo del proyecto

El objetivo es simular un flujo realista de logística de fulfillment donde múltiples componentes especializados colaboran para llevar una orden desde su recepción hasta su despacho.

Este proyecto busca demostrar conocimientos en:

- diseño de microservicios,
- arquitectura hexagonal,
- consistencia eventual,
- integración entre servicios,
- seguridad con JWT/Cognito,
- persistencia NoSQL en AWS,
- mensajería asíncrona,
- manejo de estados de negocio,
- idempotencia y control de concurrencia.

---

## Arquitectura general

El sistema está compuesto por los siguientes servicios:

1. **order-service**: crea la orden y registra el evento inicial `OrderReceived`.
2. **warehouse-service**: administra bodegas y publica eventos operativos de picking y packing.
3. **inventory-service**: administra stock, disponibilidad, reservas, consumos y liberaciones.
4. **shipping-service**: crea envíos, genera guía PDF, la sube a S3 y publica `ShipmentShipped`.
5. **order-state-processor**: consumidor principal de eventos, encargado de orquestar la evolución del estado de la orden.
6. **outbox-publisher-lambda**: proceso desacoplado que lee eventos pendientes desde tablas outbox y los publica en SQS.

---

## Microservicios

### 1. order-service

Responsable de la creación y consulta de órdenes.

**Responsabilidades principales:**
- Crear órdenes nuevas.
- Aplicar idempotencia por encabezado Idempotency-Key.
- Registrar estado inicial RECEIVED.
- Persistir un evento OrderReceived en outbox.
- Aceptar solicitudes de cancelación publicando OrderCancelled.

**Endpoints principales:**
POST /api/v1/orders
GET /api/v1/orders/{id}
POST /api/v1/orders/{id}/cancel

### 2. warehouse-service

Responsable de administrar las bodegas y emitir eventos operativos asociados a la preparación de órdenes.

**Responsabilidades principales:**
- Crear bodegas.
- Consultar una bodega o listar todas.
- Verificar existencia de bodega vía endpoint interno.
- Publicar eventos PickingCompleted y PackingCompleted.

**Endpoints principales:**
POST /api/v1/warehouses
GET /api/v1/warehouses
GET /api/v1/warehouses/{id}
POST /api/v1/warehouses/{warehouseId}/orders/{orderId}/picking/complete
POST /api/v1/warehouses/{warehouseId}/orders/{orderId}/packing/complete
HEAD /internal/v1/warehouses/{id}
GET /internal/v1/warehouses

### 3. inventory-service

Responsable del inventario por bodega y SKU.

**Responsabilidades principales:**
- Reabastecer inventario por lote.
- Consultar inventario por bodega.
- Consultar disponibilidad para una lista de SKUs.
- Reservar stock para una orden.
- Consumir una reserva cuando el proceso avanza.
- Liberar una reserva cuando una orden se cancela.

**Endpoints principales:**

Públicos
POST /api/v1/warehouses/{warehouseId}/inventory/restock
GET /api/v1/warehouses/{warehouseId}/inventory

Internos
POST /internal/v1/warehouses/{warehouseId}/inventory/availability
POST /internal/v1/warehouses/{warehouseId}/reservations
DELETE /internal/v1/reservations/{reservationId}
POST /internal/v1/reservations/{reservationId}/consume

### 4. shipping-service

Responsable de la creación del envío y administración de su ciclo de vida.

**Responsabilidades principales:**
- Crear un shipment para una orden ya empacada.
- Generar una guía PDF de envío.
- Subir la guía a S3.
- Exponer una URL prefirmada para descarga.
- Marcar shipments como SHIPPED o DELIVERED.
- Publicar evento ShipmentShipped.

**Endpoints principales:**

Internos
POST /internal/v1/shipments

Públicos
GET /api/v1/shipments/{id}
GET /api/v1/shipments
POST /api/v1/shipments/{id}/ship
POST /api/v1/shipments/{id}/deliver
GET /api/v1/shipments/{id}/guide

### 5. order-state-processor

Es el componente que procesa eventos desde SQS y actualiza el estado global de la orden.

**Responsabilidades principales:**
- Consumir mensajes desde SQS.
- Determinar el tipo de evento.
- Delegar a un handler especializado.
- Actualizar orden e historial.
- Orquestar llamadas internas a inventory, warehouse y shipping.

**Eventos procesados:**
- OrderReceived
- OrderCancelled
- PickingCompleted
- PackingCompleted
- ShipmentShipped

### 6. outbox-publisher-lambda

Componente serverless encargado de desacoplar persistencia de negocio y publicación de eventos.

**Responsabilidades principales:**
- Consultar eventos en estado PENDING desde la tabla outbox.
- Publicar esos eventos en SQS.
- Marcar como SENT los eventos exitosos.
- Marcar como FAILED los eventos con error.

---

## Flujo funcional end-to-end

### 1. Creación de orden

El cliente crea una orden mediante order-service.

### 2. Persistencia inicial

Se guarda:
la orden en estado RECEIVED,
su historial inicial,
un evento outbox OrderReceived.

### 3. Publicación del evento

La Lambda de outbox detecta el evento pendiente y lo publica en SQS.

### 4. Procesamiento de validación

order-state-processor consume OrderReceived, consulta bodegas, revisa disponibilidad y reserva inventario en la mejor bodega posible.

### 5. Cambio a VALIDATED o REJECTED

Si hay stock y reserva exitosa, la orden pasa a VALIDATED.
Si ninguna bodega puede cumplir, pasa a REJECTED.

### 6. Operación de bodega

Desde warehouse-service se notifican los hitos:
  PickingCompleted
  PackingCompleted

### 7. Consumo de reserva y creación de shipment

Al recibir PackingCompleted, el processor:
  consume la reserva,
  solicita creación de shipment a shipping-service,
  mueve la orden a PACKED.

### 8. Despacho

Cuando el shipment se marca como SHIPPED, shipping-service publica ShipmentShipped.

### 9. Cierre del ciclo

El processor consume el evento y la orden pasa a SHIPPED.

### 10. Cancelación

En cualquier punto cancelable, order-service puede publicar OrderCancelled, provocando liberación de reserva y transición a CANCELED.

---

## Modelo de estados de la orden
```
    RECEIVED    ->    VALIDATED -> PICKED -> PACKED -> SHIPPED
        |                |          |
        v                v          v
  REJECTED/CANCELED   CANCELED   CANCELED
```

## Estados

- **RECEIVED:** orden recién creada.
- **VALIDATED:** se asignó una bodega y se reservó inventario.
- **PICKED:** la bodega completó picking.
- **PACKED:** la bodega completó packing y se creó shipment.
- **SHIPPED:** el shipment fue despachado.
- **REJECTED:** no fue posible atender la orden.
- **CANCELED:** la orden fue cancelada antes del despacho.

---

## Patrones de arquitectura usados

### Arquitectura Hexagonal

Cada servicio separa:
dominio,
aplicación,
infraestructura,
puertos y adaptadores.

### Outbox Pattern

Los servicios productores escriben eventos a una tabla outbox antes de publicarlos a la cola.

### Event-Driven Architecture

La evolución del flujo depende de eventos de dominio consumidos de manera asíncrona.

### Idempotencia

Se usa para evitar duplicados tanto en entrada HTTP como en procesamiento de eventos.

### Consistencia eventual

El estado global del sistema se sincroniza de forma eventual entre servicios desacoplados.

---

Tecnologías utilizadas:

- Java 17+
- Spring Boot
- Spring Web / WebFlux
- Spring Security
- JWT / OAuth2 Resource Server
- Amazon Cognito
- Amazon DynamoDB
- Amazon SQS
- AWS Lambda
- Amazon S3
- Redis
- Apache PDFBox
- AWS SDK v2
- Project Reactor

---

## Seguridad

La seguridad se implementa con JWT Bearer Tokens y autorización por roles derivados de grupos de Amazon Cognito.

Roles en el proyecto:
- ADMIN
- OPERATOR
- WAREHOUSE_MANAGER

Ejemplos de restricciones:
- creación de bodegas: solo ADMIN
- consulta de órdenes: OPERATOR y ADMIN
- operaciones de inventario: WAREHOUSE_MANAGER y ADMIN
- endpoints internos: usados para comunicación entre servicios

---

## Persistencia y mensajería

### **DynamoDB**
Tablas utilizadas según el servicio:
- Orders
- OrderStateHistory
- Inventory
- InventoryReservations
- Warehouses
- Shipments
- OutboxEvents

### **Redis**
Usado en order-service para idempotencia de creación de órdenes.

### **SQS**
Usado como canal central de eventos entre productores y order-state-processor.

### **S3**
Usado por shipping-service para almacenar guías PDF.

---

## APIs principales

**Crear orden**
```bash
POST /api/v1/orders
Idempotency-Key: abc-123
Content-Type: application/json

{
  "lat": 4.7110,
  "lng": -74.0721,
  "items": [
    { "sku": "SKU-1", "quantity": 2 },
    { "sku": "SKU-2", "quantity": 1 }
  ]
}
```

**Consultar orden**
```bash
GET /api/v1/orders/{orderId}
```

**Cancelar orden**
```bash
POST /api/v1/orders/{orderId}/cancel
```

**Crear bodega**
```bash
POST /api/v1/warehouses
Content-Type: application/json

{
  "city": "Bogota",
  "lat": 4.7110,
  "lng": -74.0721
}
```

**Reabastecer inventario**
```bash
POST /api/v1/warehouses/{warehouseId}/inventory/restock
Content-Type: application/json

{
  "items": [
    { "sku": "SKU-1", "quantity": 50 },
    { "sku": "SKU-2", "quantity": 25 }
  ]
}
```

**Marcar picking completado**
```bash
POST /api/v1/warehouses/{warehouseId}/orders/{orderId}/picking/complete
```

**Marcar packing completado**
```bash
POST /api/v1/warehouses/{warehouseId}/orders/{orderId}/packing/complete
```

**Consultar shipment**
```bash
GET /api/v1/shipments/{shipmentId}
```

**Marcar shipment como enviado**
```bash
POST /api/v1/shipments/{shipmentId}/ship
```

**Obtener guía**
```bash
GET /api/v1/shipments/{shipmentId}/guide
```

---

## Idempotencia y concurrencia

### Idempotencia de creación de órdenes

order-service usa Idempotency-Key y Redis para evitar órdenes duplicadas ante reintentos del cliente.

### Idempotencia de eventos

Los eventos tienen eventId, y los servicios intentan evitar duplicados en outbox.

### Actualizaciones condicionales

En varios puntos se usan guardas por estado esperado para evitar transiciones inválidas o carreras concurrentes.

### Consistencia eventual

Dado que la arquitectura usa eventos asíncronos, el sistema no busca consistencia fuerte global, sino coordinación eventual entre servicios desacoplados.

---

## Mejoras futuras

- Añadir trazabilidad distribuida.
- Mejorar modelado Dynamo con GSIs donde actualmente hay scans.
- Fortalecer reclamación atómica en outbox antes de publicar eventos.
- Endurecer idempotencia completa en consumidores y productores.
- Mejorar pruebas de carga y resiliencia.
- Añadir métricas y dashboards.
- Refinar contratos internos entre servicios.
- Agregar documentación OpenAPI/Swagger.
