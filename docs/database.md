# Base de datos de SmartTraffic

PostgreSQL 16 guarda el historial de lecturas de los sensores. El esquema lo
define [`database/init.sql`](../database/init.sql), no Hibernate
(`spring.jpa.hibernate.ddl-auto: validate`).

## 1. Esquema

Base de datos: `smarttraffic`. Tabla: `traffic_readings_history`.

| Columna | Tipo | Restricción | Descripción |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Identificador interno |
| `event_id` | `UUID` | `NOT NULL`, `UNIQUE` | UUID generado por el simulador; evita duplicados |
| `zone_id` | `VARCHAR(50)` | `NOT NULL`, `CHECK (btrim(zone_id) <> '')` | Zona, por ejemplo `Zona A` |
| `vehicle_count` | `INTEGER` | `NOT NULL`, `CHECK (vehicle_count >= 0)` | Vehículos detectados |
| `reading_timestamp` | `TIMESTAMPTZ` | `NOT NULL` | Instante de la lectura (campo `timestamp` del JSON, UTC) |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` | Instante en que se guardó |

Índices:

| Índice | Columnas | Uso |
|---|---|---|
| `traffic_readings_history_pkey` | `id` | Llave primaria |
| `uk_traffic_readings_event_id` | `event_id` | Unicidad y búsqueda de duplicados |
| `idx_traffic_readings_zone_timestamp` | `zone_id, reading_timestamp` | Historial por zona y rango de fechas |

Las columnas están documentadas en la base con `COMMENT ON` (ver con
`\d+ traffic_readings_history`).

## 2. Decisiones

- **`event_id UNIQUE` (idempotencia):** Kafka entrega "al menos una vez"; tras
  un reinicio puede repetir un mensaje. La restricción hace que el segundo
  intento falle y el archiver lo ignore, así el historial nunca se duplica.
- **`TIMESTAMPTZ`:** los sensores envían UTC (`...Z`); con zona horaria no hay
  ambigüedad.
- **`reading_timestamp` en lugar de `timestamp`:** `timestamp` es un nombre de
  tipo en SQL.
- **Dos fechas:** `reading_timestamp - created_at` es la latencia del sistema.
- **Índice compuesto `(zone_id, reading_timestamp)`:** primero la columna de
  igualdad y después la de rango. `EXPLAIN` confirma `Index Scan`.
- **`CHECK` en la base además de validar en Java:** defensa en profundidad.

## 3. Usuarios (mínimo privilegio)

| Usuario | Permisos | Usado por |
|---|---|---|
| `smarttraffic_admin` | Superusuario | Solo `init.sql` y mantenimiento |
| `archiver_user` | `CONNECT`, `SELECT`, `INSERT`, uso de la secuencia | `traffic-archiver` |
| `dashboard_reader` | `CONNECT`, `SELECT` | `user-dashboard-backend` |

`init.sql` revoca los permisos por defecto de `PUBLIC`. Las contraseñas **no**
están en el archivo: se leen del entorno del contenedor con `\getenv`
(`ARCHIVER_DB_PASSWORD`, `DASHBOARD_DB_PASSWORD`), que a su vez vienen de
`.env`.

## 4. Inicialización

La base se puede levantar **sola**, sin ningún otro servicio:

```powershell
docker compose -f database/docker-compose.yml up -d
```

`init.sql` se monta en `/docker-entrypoint-initdb.d/01-init.sql` y Postgres lo
ejecuta **solo la primera vez**, con el volumen de datos vacío. Si se cambia
el esquema hay que recrear el volumen (esto **borra los datos**):

```powershell
docker compose -f database/docker-compose.yml down -v     # desarrollo
docker compose down -v                                     # sistema completo
```

## 5. Consultas

Historial de una zona en un rango (la que usa el dashboard):

```sql
SELECT event_id, zone_id, vehicle_count, reading_timestamp, created_at
FROM traffic_readings_history
WHERE zone_id = 'Zona A'
  AND reading_timestamp BETWEEN '2026-11-22T00:00:00Z' AND '2026-11-22T01:00:00Z'
ORDER BY reading_timestamp DESC
LIMIT 500;
```

Lecturas por zona:

```sql
SELECT zone_id, count(*), avg(vehicle_count)::numeric(6,2), max(reading_timestamp)
FROM traffic_readings_history GROUP BY zone_id ORDER BY zone_id;
```

Latencia promedio entre el sensor y la base:

```sql
SELECT avg(created_at - reading_timestamp) FROM traffic_readings_history;
```

## 6. Cómo conectarse

Sistema completo (Compose de la raíz; la base no publica puertos):

```powershell
docker compose exec postgres psql -U smarttraffic_admin -d smarttraffic
```

Entorno de desarrollo (`database/docker-compose.yml`, puerto `127.0.0.1:5433` en el host):

```powershell
docker exec -it smarttraffic-postgres psql -U smarttraffic_admin -d smarttraffic
```

Desde un cliente gráfico (DBeaver, IntelliJ): `localhost:5433`, base
`smarttraffic`, usuario `dashboard_reader` (solo lectura).

## 7. Pruebas del esquema

[`database/test-queries.sql`](../database/test-queries.sql) prueba inserción,
duplicado, `ON CONFLICT`, los `CHECK`, el uso del índice y los permisos de cada
usuario. Usa la zona ficticia `Zona TEST` y la borra al final.

```powershell
docker cp database/test-queries.sql smarttraffic-postgres:/tmp/test-queries.sql
docker exec smarttraffic-postgres psql -U smarttraffic_admin -d smarttraffic -f /tmp/test-queries.sql
```

Resultado verificado: las 12 comprobaciones se comportan como se espera
(inserciones válidas aceptadas; duplicado, cantidad negativa, zona vacía,
`INSERT` del dashboard y `DELETE` del archiver rechazados).
