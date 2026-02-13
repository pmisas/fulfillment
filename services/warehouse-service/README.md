# order-service

Microservicio encargado de los centros logísticos (warehouses) para plataforma de fulfillment.

---

## Requisitos

Java 17
Maven (o usar el wrapper mvnw incluido)

---

## Cómo correrlo (local)

Desde la raíz del repositorio:

### Windows (Git Bash / PowerShell)

```bash

cd services/warehouse-service
mvnw.cmd spring-boot:run
Linux / Mac
cd services/warehouse-service
./mvnw spring-boot:run
```


El servicio quedará disponible en:
http://localhost:8080

> Nota: más adelante se cambiará el puerto para correr varios servicios a la vez.


## Endpoints (planeados)

### Warehouses

#### POST /warehouses
Crea un warehouse Debe validar:

- `location` obligatorio
- no permitir duplicados


#### GET /warehouses
Retorna la lista de warehouses registrados.

#### GET /warehouses/{id}
Retorna un warehouses por id

## Persistencia 

### DynamoDB

- Tabla: `warehouses`
- Primary Key: `warehouse_id`

## Estado del proyecto

 - [x] Esqueleto Spring Boot creado
 - [ ] Endpoints implementados
 - [ ] DynamoDB integración
 - [ ] Deploy en ECS Fargate
