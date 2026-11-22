# Docker y ejecución de SmartTraffic

## 1. Archivos Compose

| Archivo | Para qué | Puertos publicados |
|---|---|---|
| [`docker-compose.yml`](../docker-compose.yml) (raíz) | **Sistema completo** para la entrega y la demostración | Solo Nginx (`80`) |
| [`database/docker-compose.yml`](../database/docker-compose.yml) | **Desarrollo**: PostgreSQL independiente (Integrante 3) | `127.0.0.1:5433` |
| `infrastructure/docker-compose.yml` | **Desarrollo**: Kafka y RabbitMQ (Integrante 1, rama `feature/daniel-kafka`) | `9092`, `5672`, `15672` |

No levantar el sistema completo junto con los de desarrollo: duplicaría
Kafka, RabbitMQ y Postgres.

## 2. Sistema completo

Requisitos: Docker Desktop (unos 4 GB libres de RAM), el puerto 80 libre y
los servicios del Integrante 1 en `services/` (es decir, `develop` con la rama
`feature/daniel-kafka` ya integrada).

```powershell
Copy-Item .env.example .env       # una sola vez; cambiar las contraseñas
docker compose --env-file .env up -d --build
```

Abrir <http://localhost>. La primera construcción tarda varios minutos
(descarga de dependencias de Maven).

| Acción | Comando |
|---|---|
| Ver estado | `docker compose ps` |
| Ver logs de un servicio | `docker compose logs -f traffic-archiver` |
| Detener (conserva datos) | `docker compose down` |
| Detener y **borrar datos** | `docker compose down -v` |
| Reconstruir un servicio | `docker compose up -d --build user-dashboard-backend` |
| Consumo de recursos | `docker stats` |
| Incluir servicios de Juan | `docker compose --profile juan up -d --build` |

### Servicios

| Servicio | Imagen | Depende de (healthy) | Healthcheck | Memoria |
|---|---|---|---|---|
| `kafka` | `apache/kafka:3.9.2` (KRaft) | — | `kafka-broker-api-versions.sh` | 1 GB |
| `rabbitmq` | `rabbitmq:4.1.8-management` | — | `rabbitmq-diagnostics ping` | 512 MB |
| `postgres` | `postgres:16` | — | `pg_isready` | 384 MB |
| `traffic-sensor-simulator` | build local | kafka | — | 512 MB |
| `traffic-density-processor` | build local | kafka, rabbitmq | — | 512 MB |
| `traffic-archiver` | build local | kafka, postgres | — | 512 MB |
| `user-dashboard-backend` | build local | rabbitmq, postgres | `/actuator/health/liveness` | 512 MB |
| `nginx` | `nginx:1.27-alpine` | user-dashboard-backend | `/nginx-health` | 64 MB |

Todos usan `restart: unless-stopped` y rotación de logs (`json-file`, 3 × 10 MB).
Los servicios Java usan `SPRING_PROFILES_ACTIVE=docker` y
`JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75`.

### Redes

```text
            navegador ──:80──▶ nginx                          (frontend-net)
                                 │
                       user-dashboard-backend                  (ambas redes)
                                 │
   backend-net (internal: true): kafka, rabbitmq, postgres, simulador, procesador, archiver
```

- `backend-net` es **interna**: sus contenedores no publican puertos ni salen
  a Internet.
- Nginx no puede ver a Kafka ni a Postgres (verificado: "bad address").
- Dentro de la red, los servicios se encuentran por nombre (`kafka:29092`,
  `rabbitmq:5672`, `postgres:5432`).

### Variables (`.env`)

| Variable | Uso |
|---|---|
| `HTTP_PORT` | Puerto público de Nginx (80) |
| `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | Usuario de RabbitMQ (procesador, dashboard, Juan) |
| `POSTGRES_DB`, `POSTGRES_ADMIN_USER`, `POSTGRES_ADMIN_PASSWORD` | Administrador de Postgres |
| `ARCHIVER_DB_USER`, `ARCHIVER_DB_PASSWORD` | Usuario de escritura del archiver |
| `DASHBOARD_DB_USER`, `DASHBOARD_DB_PASSWORD` | Usuario de solo lectura del dashboard |
| `DASHBOARD_ALLOWED_ORIGINS` | Orígenes CORS si el frontend se sirve fuera de Nginx |

Las contraseñas son obligatorias (`${VAR:?...}`): sin `.env` el Compose no
arranca. `.env` está en `.gitignore`.

## 3. Nginx

[`nginx/nginx.conf`](../nginx/nginx.conf):

| Ruta | Destino |
|---|---|
| `/` | `frontend/dashboard.html` |
| `/api/` | `user-dashboard-backend:8080` (rate limit 20 r/s, ráfaga 40) |
| `/ws/` | `user-dashboard-backend:8080` con `Upgrade`/`Connection` y timeout de 1 h |

Endurecimiento: `server_tokens off`, timeouts de 10 s (Slowloris),
`limit_req_zone`, `X-Content-Type-Options`, `X-Frame-Options`,
`Referrer-Policy` y bloqueo de archivos ocultos (`/.env`, `/.git`).

Comprobaciones:

```powershell
curl.exe -I http://localhost/                      # cabeceras de seguridad
curl.exe -s http://localhost/api/stats
curl.exe -s -o NUL -w "%{http_code}" http://localhost/.env    # 404
curl.exe -s -i -N --max-time 8 -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" http://localhost/ws/traffic
```

La última muestra `HTTP/1.1 101` (Switching Protocols) y los mensajes JSON.

## 4. Dockerfiles

Los cuatro servicios Java siguen el mismo patrón:

1. Etapa de build `maven:3.9.9-eclipse-temurin-17`: primero `COPY pom.xml` y
   `dependency:go-offline` (caché de dependencias), después `COPY src` y
   `package`.
2. Etapa de ejecución `eclipse-temurin:17-jre`: solo el JAR, usuario
   `smarttraffic` UID/GID 1001 (no root), `ENTRYPOINT java -jar app.jar`.
3. `.dockerignore` excluye `target/`, `.git/`, `.env` y archivos del IDE.

## 5. Desarrollo local (sin el stack completo)

Base de datos (funciona sola, sin otros servicios):

```powershell
docker compose -f database/docker-compose.yml up -d
```

Kafka y RabbitMQ (Compose del Integrante 1):

```powershell
cd infrastructure; docker compose up -d; cd ..
```

Después, cada servicio en su terminal con `mvn spring-boot:run` (perfil por
defecto, `localhost`):

| Servicio | Puerto/conexión en el host |
|---|---|
| Kafka | `localhost:9092` |
| RabbitMQ / consola | `localhost:5672` / <http://localhost:15672> |
| PostgreSQL | `localhost:5433` |
| user-dashboard-backend | `SERVER_PORT=8081` si el 8080 está ocupado |
| dashboard.html | `python -m http.server 5500 --directory frontend` y abrir <http://localhost:5500/dashboard.html> (usa la API en `localhost:8081`) |

## 6. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `Port 8080 was already in use` | Otro programa (p. ej. Apache de XAMPP) usa el 8080. En el host: `$env:SERVER_PORT="8081"`. En Docker no afecta |
| Postgres no arranca en `5432` | Hay un PostgreSQL local en Windows; el Compose de desarrollo usa `5433` |
| Cambié `init.sql` y no se aplica | Solo corre con el volumen vacío: `docker compose down -v` (o `docker compose -f database/docker-compose.yml down -v` en desarrollo) |
| `unable to prepare context ... traffic-sensor-simulator` | Faltan los servicios del Integrante 1: integrar `feature/daniel-kafka` en `develop` |
| `container name "/smarttraffic-postgres" is already in use` | Quedó un contenedor viejo con ese nombre: `docker rm smarttraffic-postgres` (los datos están en el volumen) |
| `required variable ... is missing` | Falta `.env`: `Copy-Item .env.example .env` |
| El dashboard dice "Esperando datos" | El procesador todavía no publicó zonas; revisar `docker compose logs traffic-density-processor` |
| El dashboard dice "Sin conexión" | El backend se está reiniciando; el navegador reintenta solo |
| `mvn` no se reconoce | Abrir una terminal nueva o recargar el PATH |

## 7. Evidencia con Wireshark (para la defensa)

| Filtro | Qué se ve |
|---|---|
| `tcp.port == 5672` o `amqp` | `Basic.Publish` del procesador y `Basic.Deliver` al dashboard. El tráfico entre contenedores no pasa por Windows: para verlo, usar el Compose de desarrollo y correr procesador y dashboard en el host con `mvn spring-boot:run` |
| `http.upgrade` | Handshake WebSocket: `GET /ws/traffic` con `Upgrade: websocket` → `101` |
| `websocket` | Frames de texto con el JSON cada 5 s |

En Windows, para capturar tráfico de `localhost` se necesita el adaptador
"Adapter for loopback traffic capture" (Npcap).
