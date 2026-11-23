-- =============================================================================
-- SmartTraffic - Migración 001: tabla traffic_status_history
-- -----------------------------------------------------------------------------
-- Para bases creadas ANTES de que init.sql incluyera el historial de estados.
-- Crea la tabla, su índice y sus permisos SIN borrar los datos existentes.
-- Una base nueva no la necesita: init.sql ya crea todo.
--
-- Es idempotente: se puede ejecutar varias veces sin error ni duplicados.
-- Se ejecuta dentro del contenedor de Postgres, que tiene las variables
-- ARCHIVER_DB_USER y DASHBOARD_DB_USER (ver docs/database.md, sección
-- "Migraciones").
-- =============================================================================

\set ON_ERROR_STOP on

\getenv archiver_user  ARCHIVER_DB_USER
\getenv dashboard_user DASHBOARD_DB_USER

BEGIN;

CREATE TABLE IF NOT EXISTS traffic_status_history (
    id                BIGSERIAL        PRIMARY KEY,
    zone_id           VARCHAR(50)      NOT NULL,
    previous_status   VARCHAR(20),
    traffic_status    VARCHAR(20)      NOT NULL,
    average_vehicles  DOUBLE PRECISION NOT NULL,
    status_timestamp  TIMESTAMPTZ      NOT NULL,
    created_at        TIMESTAMPTZ      NOT NULL DEFAULT now(),

    CONSTRAINT ck_traffic_status_value
        CHECK (traffic_status IN ('DESPEJADA', 'MODERADA', 'CONGESTIONADA')),
    CONSTRAINT ck_traffic_status_previous_value
        CHECK (previous_status IS NULL
               OR previous_status IN ('DESPEJADA', 'MODERADA', 'CONGESTIONADA')),
    CONSTRAINT ck_traffic_status_average CHECK (average_vehicles >= 0),
    CONSTRAINT ck_traffic_status_zone_not_blank CHECK (btrim(zone_id) <> '')
);

CREATE INDEX IF NOT EXISTS idx_traffic_status_zone_timestamp
    ON traffic_status_history (zone_id, status_timestamp);

COMMENT ON TABLE traffic_status_history IS
    'Historial de cambios de estado de congestión por zona (exchange traffic_updates)';
COMMENT ON COLUMN traffic_status_history.previous_status IS
    'Estado anterior de la zona; NULL si es la primera vez que se registra';
COMMENT ON COLUMN traffic_status_history.traffic_status IS
    'Estado nuevo: DESPEJADA, MODERADA o CONGESTIONADA';
COMMENT ON COLUMN traffic_status_history.average_vehicles IS
    'Promedio de vehículos de la ventana en el momento del cambio';
COMMENT ON COLUMN traffic_status_history.status_timestamp IS
    'Instante del snapshot publicado por el procesador (UTC)';

-- GRANT es idempotente: repetirlo no cambia nada.
GRANT SELECT, INSERT ON traffic_status_history TO :"archiver_user";
GRANT USAGE ON SEQUENCE traffic_status_history_id_seq TO :"archiver_user";
GRANT SELECT ON traffic_status_history TO :"dashboard_user";

COMMIT;
