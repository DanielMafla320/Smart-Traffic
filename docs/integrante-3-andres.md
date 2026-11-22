# Integrante 3 (Andrés): PostgreSQL, historial, dashboard y Docker

## Inicio rápido del sistema completo

Requisitos: Docker Desktop, el puerto 80 libre y `develop` con los servicios
del Integrante 1 (`feature/daniel-kafka`) ya integrados.

```powershell
Copy-Item .env.example .env        # Bash: cp .env.example .env
docker compose --env-file .env up -d --build
```

Abrir <http://localhost>: dashboard con las cinco zonas en tiempo real e
historial. Detener con `docker compose down` (`down -v` borra los datos).

| Integrante | Componentes |
|---|---|
| 1 · Daniel | `traffic-sensor-simulator`, `traffic-density-processor`, `infrastructure/` |
| 2 · Juan | `alert-dispatcher`, `query-client` (perfil `juan` del Compose) |
| 3 · Andrés | `database/`, `traffic-archiver`, `user-dashboard-backend`, `frontend/dashboard.html`, `docker-compose.yml`, `nginx/` |

## Componentes

| Componente | Qué hace | Pruebas |
|---|---|---|
| [`database/`](../database) | `init.sql` (tabla `traffic_readings_history` con `event_id UNIQUE`, `CHECK`, índice `(zone_id, reading_timestamp)` y usuarios de mínimo privilegio) y un `docker-compose.yml` para levantar la base sola | `database/test-queries.sql` (12 comprobaciones) |
| [`services/traffic-archiver`](../services/traffic-archiver) | Consume `raw_traffic_data` (grupo `traffic-archiver`) y guarda cada lectura con JPA; ignora duplicados; tolera JSON inválido y caídas de la base sin perder lecturas | 28 unitarias |
| [`services/user-dashboard-backend`](../services/user-dashboard-backend) | Cola `dashboard_traffic_queue` en `traffic_updates`; `GET /api/zones`, `/api/zones/detail`, `/api/stats`, `/api/history`; WebSocket `/ws/traffic` | 43 unitarias (incluye `@WebMvcTest` y CORS) |
| [`frontend/dashboard.html`](../frontend/dashboard.html) | Tarjetas por zona, resumen e historial con filtros; REST + WebSocket con reconexión | Probado en navegador |
| [`docker-compose.yml`](../docker-compose.yml), [`nginx/`](../nginx) | Sistema completo; solo Nginx publica puerto; `backend-net` interna | Verificado de extremo a extremo |

Pruebas:

```powershell
cd services/traffic-archiver; mvn test
cd ../user-dashboard-backend; mvn test
```

## Documentación

- [`architecture.md`](architecture.md): arquitectura, contratos, API REST y
  WebSocket, patrones, tolerancia a fallos y CAP.
- [`database.md`](database.md): esquema, índices, usuarios e idempotencia.
- [`docker.md`](docker.md): Compose, Nginx, redes, variables, desarrollo local
  y solución de problemas.
