# E2E tests

Prueba E2E Java minima para ejecutar desde tu maquina contra AWS real.

- Crea una orden por HTTP.
- Valida que la respuesta inmediata sea `RECEIVED`.
- Hace polling al endpoint de orden hasta que el flujo real asincrono la lleve al estado esperado, por defecto `VALIDATED`.

espera el cambio real producido por outbox, EventBridge, SQS, worker y servicios desplegados.

## Configuracion

```powershell
Copy-Item .env.example .env
```

Edita `.env` con tu URL real, token y datos del fixture.

## Ejecutar

Desde `fulfillment/e2e-tests`:

```powershell
..\services\order-service\mvnw.cmd -f pom.xml test
```

Necesita JDK 17. Si `java -version` muestra Java 8 o `javac` no existe, Maven no podra compilar.
