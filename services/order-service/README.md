# order-service

Microservicio encargado de recibir y consultar órdenes para la plataforma de fulfillment.

---

## Requisitos

Java 17
Maven (o usar el wrapper mvnw incluido)

---

## Cómo correrlo (local)

Desde la raíz del repositorio:

### Windows (Git Bash / PowerShell)

```bash

cd services/order-service
mvnw.cmd spring-boot:run
Linux / Mac
cd services/order-service
./mvnw spring-boot:run
```


El servicio quedará disponible en:
http://localhost:8080

> Nota: más adelante se cambiará el puerto para correr varios servicios a la vez (ej. 8081).


## Endpoints (planeados)

### Orders

#### POST /orders
Crea una orden. Debe validar:

- `warehouse_id` obligatorio
- `customer_id` obligatorio
- `items` no vacío
- `quantity > 0`

Idempotencia por header:
`Idempotency-Key`

#### GET /orders/{id}
Retorna una orden por id.

## Persistencia 

### DynamoDB

- Tabla: `Orders`
- Primary Key: `order_id`

### Redis (más adelante)

- Idempotencia (`Idempotency-Key`)
- Rate limiting

## Estado del proyecto

 - [x] Esqueleto Spring Boot creado
 - [x] Endpoints implementados
 - [ ] DynamoDB integración
 - [ ] Redis idempotencia
 - [ ] Deploy en ECS Fargate
