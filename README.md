# Fulfillment 

Plataforma backend de tipo **fulfillment/logística**, diseñada bajo arquitectura de **microservicios**, aplicando principios de:

- Arquitectura Hexagonal (Ports & Adapters)
- Diseño preparado para AWS (ECS + DynamoDB + Redis)
- Idempotencia y consistencia eventual (roadmap)

---

## Arquitectura General

El proyecto está organizado como un **monorepo** con múltiples microservicios:


Cada microservicio:

- Es una aplicación Spring Boot independiente
- Expone API REST
- Implementa arquitectura hexagonal
- Usa persistencia en memoria (temporal)
- Está preparado para migrar a DynamoDB y Redis

---

## Contexto del Dominio

La plataforma simula un sistema de fulfillment donde:

- Un cliente crea órdenes
- Las órdenes pertenecen a un warehouse
- Cada orden contiene múltiples items
- Los warehouses gestionan centros logísticos

En etapas futuras se agregarán:

- Servicio de inventario
- Worker de cambios de estado
- Reposición automática entre warehouses
- Idempotencia con Redis
- Persistencia en DynamoDB
- Integración con AWS ECS Fargate

---

# Microservicios

---

## 1️. Order Service

Responsable de:

- Crear órdenes
- Consultar órdenes por ID
- Validar reglas de negocio básicas
- Manejar estructura de Order + OrderItem
- Manejo centralizado de errores

###  Endpoints

#### POST /api/v1/orders

Crea una nueva orden.

`{   "warehouseId": "wh-001",   "customerId": "cust-001",   "items": [     { "sku": "SKU-APPLE-01", "quantity": 2 },     { "sku": "SKU-BANANA-02", "quantity": 1 }   ] }`

Response (201 Created):

`{   "orderId": "uuid",   "customerId": "cust-001",   "status": "CREATED",   "items": [     { "sku": "SKU-APPLE-01", "quantity": 2 },     { "sku": "SKU-BANANA-02", "quantity": 1 }   ] }`

---

#### GET /api/v1/orders/{id}

Obtiene una orden por ID.

Response 200:

`{   "orderId": "uuid",   "warehouseId": "wh-001",   "customerId": "cust-001",   "status": "CREATED",   "items": [...] }`

Response 404:

`{   "code": "ORDER_NOT_FOUND",   "message": "Order not found: 123",   "timestamp": "...",   "path": "/api/v1/orders/123" }`

---

### Modelo de Dominio

- Order (Aggregate Root)
- OrderItem (Value Object dentro de Order)

No existe tabla separada para items.  
Los items viven dentro del agregado Order.

---

### Arquitectura

``` bash
application/
  dto/
  port/
  service/
      
  
domain/
  exception/
  model/
  port/


infrastructure/
  rest/
      controller/
      dto/
      mapper/
  repository/
      dynamodb/
      memory/
```

Se utilizan:

- Ports (interfaces)
- Adapters in-memory
- Mappers REST ↔ Domain y Domain ↔ Response
- DTOs  especifos de API y DTOs internos para casos de uso(command)

---

## 2️. Warehouse Service

Responsable de:

- Crear warehouses
- Listar warehouses
- Consultar warehouse por ID
- Validar duplicados

### Endpoints

#### POST /api/v1/warehouses

`{   "warehouseId": "wh-001",   "location": "Bogotá" }`

#### GET /api/v1/warehouses

Lista todos los warehouses.

#### GET /api/v1/warehouses/{id}

Obtiene un warehouse específico.


---

# Cómo correr los servicios

Desde cada carpeta del servicio:

`cd services/order-service ./mvnw spring-boot:run`

o

`cd services/warehouse-service ./mvnw spring-boot:run`

---

# Estado Actual

Persistencia en memoria (para desarrollo)
Idempotencia pendiente de implementación (diseño preparado con Ports)
Preparado para integrar:
- DynamoDB
- Redis
- ECS Fargate
- Load Balancer
- VPC

---

# Roadmap

- [ ] Idempotencia con Redis
- [ ] Persistencia DynamoDB
- [ ] Servicio de Inventario
- [ ] Worker de cambio de estados
- [ ] Reposición automática entre warehouses
- [ ] WebFlux para procesamiento reactivo