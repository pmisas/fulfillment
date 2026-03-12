# order-state-processor

Microservicio encargado de **consumir eventos del sistema y actualizar el estado global de las órdenes**.

## Responsabilidad

Este servicio actúa como el componente orquestador del flujo logístico basado en eventos.

Sus funciones principales son:

- consumir mensajes desde SQS,
- identificar el tipo de evento recibido,
- delegar el procesamiento a un handler especializado,
- actualizar el estado de la orden,
- registrar historial de cambios,
- coordinar llamadas internas a otros microservicios.

## Funcionalidades principales

- Consumo reactivo de mensajes desde SQS
- Procesamiento por tipo de evento
- Actualización de estado de órdenes
- Registro de historial de estados
- Integración con inventory-service
- Integración con warehouse-service
- Integración con shipping-service

## Eventos procesados

- `OrderReceived`
- `OrderCancelled`
- `PickingCompleted`
- `PackingCompleted`
- `ShipmentShipped`

## Qué hace con cada evento

### OrderReceived
- Consulta las bodegas disponibles
- Evalúa disponibilidad de inventario
- Calcula un ranking de bodegas
- Intenta reservar stock en la mejor opción posible
- Si la reserva es exitosa, la orden pasa a `VALIDATED`
- Si ninguna bodega puede cumplir, la orden pasa a `REJECTED`

### PickingCompleted
- Si la orden está en `VALIDATED`, pasa a `PICKED`

### PackingCompleted
- Consume la reserva de inventario
- Solicita creación del shipment
- Si todo sale bien, la orden pasa a `PACKED`

### ShipmentShipped
- Si la orden está en `PACKED`, pasa a `SHIPPED`

### OrderCancelled
- Libera la reserva si existe
- Si la orden está en un estado cancelable, pasa a `CANCELED`

## Estados manejados

- `RECEIVED`
- `VALIDATED`
- `REJECTED`
- `PICKED`
- `PACKED`
- `SHIPPED`
- `CANCELED`

## Componentes técnicos principales

- **Spring Boot**
- **Spring WebFlux / Reactor**
- **Amazon SQS**
- **DynamoDB Async**
- **AWS SDK v2**
- Integraciones HTTP reactivas con otros servicios

## Persistencia

Este servicio trabaja principalmente con:

- tabla de órdenes
- tabla de historial de estados

## Flujo resumido

1. Recibe mensajes desde SQS.
2. Extrae `eventType`, `eventId` y `payload`.
3. Busca el handler correspondiente.
4. Ejecuta la lógica de negocio según el evento.
5. Actualiza la orden y su historial.
6. Si el procesamiento es exitoso, elimina el mensaje de la cola.

## Relación con otros servicios

- **order-service**: consume eventos publicados al crear o cancelar órdenes
- **warehouse-service**: consume eventos de picking y packing completado
- **inventory-service**: consulta disponibilidad, reserva, consume y libera inventario
- **shipping-service**: solicita creación de shipments
- **outbox-publisher-lambda**: publica en SQS los eventos que este servicio consume

## Consideraciones importantes

- Este servicio es el principal responsable de la evolución del estado de las órdenes.
- El procesamiento está basado en eventos y consistencia eventual.
- Se apoya en validaciones por estado para evitar transiciones inválidas.
- Usa handlers separados por tipo de evento para mantener la lógica desacoplada.
