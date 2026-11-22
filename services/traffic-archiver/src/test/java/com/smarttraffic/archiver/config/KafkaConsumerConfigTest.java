package com.smarttraffic.archiver.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLTransientConnectionException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * Verifica qué errores se consideran "base de datos caída" (reintento sin
 * límite) y cuáles no (reintentos limitados y descarte).
 */
class KafkaConsumerConfigTest {

    @Test
    void detectaQueNoSePuedeAbrirTransaccion() {
        Exception error = new CannotCreateTransactionException("Could not open JPA EntityManager");

        assertThat(KafkaConsumerConfig.isDatabaseUnavailable(error)).isTrue();
    }

    @Test
    void detectaElErrorDentroDeLaExcepcionDelListener() {
        // Spring Kafka envuelve la excepción original en ListenerExecutionFailedException.
        Exception error = new ListenerExecutionFailedException("Listener failed",
                new DataAccessResourceFailureException("Connection refused"));

        assertThat(KafkaConsumerConfig.isDatabaseUnavailable(error)).isTrue();
    }

    @Test
    void detectaTimeoutDelPoolDeConexiones() {
        Exception error = new ListenerExecutionFailedException("Listener failed",
                new RuntimeException(new SQLTransientConnectionException("archiver-pool - Connection is not available")));

        assertThat(KafkaConsumerConfig.isDatabaseUnavailable(error)).isTrue();
    }

    @Test
    void noConsideraCaidaUnaViolacionDeRestriccion() {
        Exception error = new DataIntegrityViolationException("ck_traffic_readings_vehicle_count");

        assertThat(KafkaConsumerConfig.isDatabaseUnavailable(error)).isFalse();
    }

    @Test
    void noConsideraCaidaUnErrorCualquiera() {
        assertThat(KafkaConsumerConfig.isDatabaseUnavailable(new IllegalStateException("bug"))).isFalse();
        assertThat(KafkaConsumerConfig.isDatabaseUnavailable(null)).isFalse();
    }
}
