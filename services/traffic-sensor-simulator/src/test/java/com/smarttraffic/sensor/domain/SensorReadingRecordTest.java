package com.smarttraffic.sensor.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SensorReadingRecordTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void unaLecturaCompletaEsValida() {
        SensorReadingRecord reading = validReading(45);

        assertThat(validator.validate(reading)).isEmpty();
    }

    @Test
    void rechazaCantidadDeVehiculosNegativaOVacia() {
        assertThat(validator.validate(validReading(-1)))
                .extracting(ConstraintViolation::getPropertyPath)
                .anySatisfy(path -> assertThat(path.toString()).isEqualTo("vehicleCount"));

        SensorReadingRecord withoutCount = new SensorReadingRecord(
                UUID.randomUUID(), "Zona A", null, Instant.parse("2026-11-12T16:00:00Z"));
        assertThat(validator.validate(withoutCount)).isNotEmpty();
    }

    @Test
    void rechazaZonaVaciaYEventoSinIdentificadorOTimestamp() {
        assertThat(validator.validate(new SensorReadingRecord(
                UUID.randomUUID(), "  ", 10, Instant.now()))).isNotEmpty();

        assertThat(validator.validate(new SensorReadingRecord(
                null, "Zona A", 10, Instant.now()))).isNotEmpty();

        assertThat(validator.validate(new SensorReadingRecord(
                UUID.randomUUID(), "Zona A", 10, null))).isNotEmpty();
    }

    @Test
    void serializaElContratoJsonConNombresYFormatoAcordados() throws Exception {
        SensorReadingRecord reading = new SensorReadingRecord(
                UUID.fromString("6f1b8e4e-6f6a-4b52-9d3a-5c1f5d7d9f10"),
                "Zona A",
                45,
                Instant.parse("2026-11-12T16:00:00Z"));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(reading));

        assertThat(json.fieldNames()).toIterable()
                .containsExactly("event_id", "zone_id", "vehicle_count", "timestamp");
        assertThat(json.get("event_id").asText()).isEqualTo("6f1b8e4e-6f6a-4b52-9d3a-5c1f5d7d9f10");
        assertThat(json.get("zone_id").asText()).isEqualTo("Zona A");
        assertThat(json.get("vehicle_count").asInt()).isEqualTo(45);
        assertThat(json.get("timestamp").asText()).isEqualTo("2026-11-12T16:00:00Z");

        assertThat(objectMapper.readValue(objectMapper.writeValueAsString(reading), SensorReadingRecord.class))
                .isEqualTo(reading);
    }

    private SensorReadingRecord validReading(int vehicleCount) {
        return new SensorReadingRecord(
                UUID.randomUUID(), "Zona A", vehicleCount, Instant.parse("2026-11-12T16:00:00Z"));
    }
}
