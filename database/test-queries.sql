-- =============================================================================
-- SmartTraffic - Consultas de prueba manual del esquema
-- -----------------------------------------------------------------------------
-- Ejecutar como administrador (desde la raíz del repositorio). Se copia el
-- archivo al contenedor para que PowerShell no altere las tildes:
--   docker cp database/test-queries.sql smarttraffic-postgres:/tmp/test-queries.sql
--   docker exec smarttraffic-postgres psql -U smarttraffic_admin -d smarttraffic -f /tmp/test-queries.sql
--
-- Usa la zona ficticia 'Zona TEST' y la borra al final: no deja basura.
-- Los pasos marcados "DEBE FALLAR" muestran un ERROR a propósito.
-- =============================================================================

\set ON_ERROR_STOP off
\echo '== 1. Insertar dos lecturas válidas =='
INSERT INTO traffic_readings_history (event_id, zone_id, vehicle_count, reading_timestamp)
VALUES ('11111111-1111-1111-1111-111111111111', 'Zona TEST', 45, '2026-11-21T13:00:00Z'),
       ('22222222-2222-2222-2222-222222222222', 'Zona TEST', 80, '2026-11-21T13:00:02Z');

\echo '== 2. Mismo event_id otra vez (DEBE FALLAR: uk_traffic_readings_event_id) =='
INSERT INTO traffic_readings_history (event_id, zone_id, vehicle_count, reading_timestamp)
VALUES ('11111111-1111-1111-1111-111111111111', 'Zona TEST', 99, '2026-11-21T13:00:04Z');

\echo '== 3. Mismo event_id con ON CONFLICT DO NOTHING (no falla, inserta 0 filas) =='
INSERT INTO traffic_readings_history (event_id, zone_id, vehicle_count, reading_timestamp)
VALUES ('11111111-1111-1111-1111-111111111111', 'Zona TEST', 99, '2026-11-21T13:00:04Z')
ON CONFLICT (event_id) DO NOTHING;

\echo '== 4. Cantidad negativa (DEBE FALLAR: ck_traffic_readings_vehicle_count) =='
INSERT INTO traffic_readings_history (event_id, zone_id, vehicle_count, reading_timestamp)
VALUES ('33333333-3333-3333-3333-333333333333', 'Zona TEST', -5, '2026-11-21T13:00:06Z');

\echo '== 5. Zona vacía (DEBE FALLAR: ck_traffic_readings_zone_not_blank) =='
INSERT INTO traffic_readings_history (event_id, zone_id, vehicle_count, reading_timestamp)
VALUES ('44444444-4444-4444-4444-444444444444', '   ', 10, '2026-11-21T13:00:08Z');

\echo '== 6. Historial por zona y rango de fechas (deben salir 2 filas) =='
SELECT event_id, zone_id, vehicle_count, reading_timestamp, created_at
FROM traffic_readings_history
WHERE zone_id = 'Zona TEST'
  AND reading_timestamp BETWEEN '2026-11-21T12:59:00Z' AND '2026-11-21T13:01:00Z'
ORDER BY reading_timestamp;

\echo '== 7. Plan de ejecución: debe usar idx_traffic_readings_zone_timestamp =='
SET enable_seqscan = off;
EXPLAIN SELECT * FROM traffic_readings_history
WHERE zone_id = 'Zona TEST'
  AND reading_timestamp BETWEEN '2026-11-21T12:59:00Z' AND '2026-11-21T13:01:00Z';
RESET enable_seqscan;

\echo '== 8. Usuario del dashboard: SELECT permitido =='
SET ROLE dashboard_reader;
SELECT count(*) AS lecturas_visibles FROM traffic_readings_history WHERE zone_id = 'Zona TEST';

\echo '== 9. Usuario del dashboard: INSERT (DEBE FALLAR: permission denied) =='
INSERT INTO traffic_readings_history (event_id, zone_id, vehicle_count, reading_timestamp)
VALUES ('55555555-5555-5555-5555-555555555555', 'Zona TEST', 10, now());
RESET ROLE;

\echo '== 10. Usuario del archiver: INSERT permitido =='
SET ROLE archiver_user;
INSERT INTO traffic_readings_history (event_id, zone_id, vehicle_count, reading_timestamp)
VALUES ('66666666-6666-6666-6666-666666666666', 'Zona TEST', 12, '2026-11-21T13:00:10Z');

\echo '== 11. Usuario del archiver: DELETE (DEBE FALLAR: permission denied) =='
DELETE FROM traffic_readings_history WHERE zone_id = 'Zona TEST';
RESET ROLE;

\echo '== 12. Limpieza de los datos de prueba (como administrador) =='
DELETE FROM traffic_readings_history WHERE zone_id = 'Zona TEST';
