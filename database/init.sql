-- =============================================================================
-- SmartTraffic - Esquema inicial de PostgreSQL
-- -----------------------------------------------------------------------------
-- Se ejecuta automáticamente UNA sola vez, cuando el contenedor de Postgres
-- arranca con el volumen de datos vacío (docker-entrypoint-initdb.d).
-- Si se modifica este archivo, hay que recrear el volumen:
--     docker compose down -v   (borra los datos)
--
-- Lo ejecuta el superusuario definido en POSTGRES_USER, conectado a la base
-- POSTGRES_DB (smarttraffic). Las credenciales de los usuarios de aplicación
-- NO están escritas aquí: se leen de variables de entorno con \getenv para no
-- versionar secretos en Git.
-- =============================================================================

-- Detener la ejecución ante el primer error (no dejar el esquema a medias).
\set ON_ERROR_STOP on

-- Leer usuarios y contraseñas de aplicación desde el entorno del contenedor.
\getenv archiver_user      ARCHIVER_DB_USER
\getenv archiver_password  ARCHIVER_DB_PASSWORD
\getenv dashboard_user     DASHBOARD_DB_USER
\getenv dashboard_password DASHBOARD_DB_PASSWORD

-- -----------------------------------------------------------------------------
-- 1. Tabla de historial de lecturas de sensores
-- -----------------------------------------------------------------------------
CREATE TABLE traffic_readings_history (
    id                BIGSERIAL    PRIMARY KEY,
    event_id          UUID         NOT NULL,
    zone_id           VARCHAR(50)  NOT NULL,
    vehicle_count     INTEGER      NOT NULL,
    reading_timestamp TIMESTAMPTZ  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Kafka entrega "al menos una vez": el mismo evento puede llegar dos veces.
    -- La restricción UNIQUE hace que guardar sea idempotente.
    CONSTRAINT uk_traffic_readings_event_id UNIQUE (event_id),

    -- Reglas de negocio garantizadas también por la base de datos.
    CONSTRAINT ck_traffic_readings_vehicle_count CHECK (vehicle_count >= 0),
    CONSTRAINT ck_traffic_readings_zone_not_blank CHECK (btrim(zone_id) <> '')
);

-- Índice compuesto para el historial por zona y rango de fechas:
--   WHERE zone_id = ? AND reading_timestamp BETWEEN ? AND ?
CREATE INDEX idx_traffic_readings_zone_timestamp
    ON traffic_readings_history (zone_id, reading_timestamp);

COMMENT ON TABLE traffic_readings_history IS
    'Historial de lecturas crudas de sensores consumidas del topic Kafka raw_traffic_data';
COMMENT ON COLUMN traffic_readings_history.id IS
    'Identificador interno autoincremental';
COMMENT ON COLUMN traffic_readings_history.event_id IS
    'UUID del evento generado por el simulador; evita duplicados';
COMMENT ON COLUMN traffic_readings_history.zone_id IS
    'Zona de la lectura, por ejemplo "Zona A"';
COMMENT ON COLUMN traffic_readings_history.vehicle_count IS
    'Cantidad de vehículos detectados (>= 0)';
COMMENT ON COLUMN traffic_readings_history.reading_timestamp IS
    'Instante de la lectura en el sensor (campo timestamp del JSON, UTC)';
COMMENT ON COLUMN traffic_readings_history.created_at IS
    'Instante en que la lectura se guardó en la base de datos';

-- -----------------------------------------------------------------------------
-- 2. Usuarios de aplicación (principio de mínimo privilegio)
-- -----------------------------------------------------------------------------
-- archiver: solo puede leer e insertar (no actualizar ni borrar).
CREATE ROLE :"archiver_user" LOGIN PASSWORD :'archiver_password';

-- dashboard: solo lectura.
CREATE ROLE :"dashboard_user" LOGIN PASSWORD :'dashboard_password';

-- Ningún usuario puede conectarse a la base salvo que se le permita.
-- :"DBNAME" es una variable propia de psql con el nombre de la base actual.
REVOKE ALL ON DATABASE :"DBNAME" FROM PUBLIC;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

GRANT CONNECT ON DATABASE :"DBNAME" TO :"archiver_user", :"dashboard_user";
GRANT USAGE ON SCHEMA public TO :"archiver_user", :"dashboard_user";

GRANT SELECT, INSERT ON traffic_readings_history TO :"archiver_user";
GRANT USAGE ON SEQUENCE traffic_readings_history_id_seq TO :"archiver_user";

GRANT SELECT ON traffic_readings_history TO :"dashboard_user";
