package com.smarttraffic.archiver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.archiver.dto.TrafficReadingEvent;
import java.sql.SQLTransientConnectionException;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Configuración del consumidor de Kafka del archivador.
 *
 * <p>Política de errores:
 * <ul>
 *   <li>JSON inválido: el {@link ErrorHandlingDeserializer} evita que el
 *       consumidor muera; el registro se descarta con un log.</li>
 *   <li>Base de datos caída: se reintenta el mismo registro sin límite, cada
 *       {@code database-retry-interval-ms}. El offset no avanza, así que no se
 *       pierde ninguna lectura; al volver la base, se continúa donde se quedó.</li>
 *   <li>Cualquier otro error inesperado: pocos reintentos y luego se descarta
 *       con log para no bloquear la partición para siempre.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    /**
     * Crea el topic si todavía no existe (mismas características que el
     * simulador). Si ya existe, Kafka no hace nada. Así el archivador puede
     * arrancar antes que el simulador.
     */
    @Bean
    public NewTopic rawTrafficDataTopic(ArchiverKafkaProperties properties) {
        return TopicBuilder.name(properties.topic())
                .partitions(properties.partitions())
                .replicas(properties.replicas())
                .build();
    }

    @Bean
    public ConsumerFactory<String, TrafficReadingEvent> trafficReadingConsumerFactory(
            KafkaProperties kafkaProperties,
            ObjectProvider<SslBundles> sslBundles,
            ObjectMapper objectMapper) {
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties(sslBundles.getIfAvailable());

        // Deserializa siempre a TrafficReadingEvent: el productor no envía __TypeId__.
        JsonDeserializer<TrafficReadingEvent> jsonDeserializer =
                new JsonDeserializer<>(TrafficReadingEvent.class, objectMapper, false);
        jsonDeserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TrafficReadingEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, TrafficReadingEvent> consumerFactory,
            CommonErrorHandler kafkaCommonErrorHandler,
            KafkaProperties kafkaProperties) {
        ConcurrentKafkaListenerContainerFactory<String, TrafficReadingEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        Integer concurrency = kafkaProperties.getListener().getConcurrency();
        factory.setConcurrency(concurrency == null ? 1 : concurrency);
        factory.setCommonErrorHandler(kafkaCommonErrorHandler);
        // Si el topic aún no existe, el consumidor espera en lugar de fallar.
        factory.setMissingTopicsFatal(false);
        // Confirmar el offset después de procesar cada registro con éxito.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public CommonErrorHandler kafkaCommonErrorHandler(ArchiverKafkaProperties properties) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> {
                    // Llega aquí un error no recuperable (p. ej. JSON inválido, que no se
                    // reintenta) o un error inesperado que agotó sus reintentos.
                    if (record == null) {
                        log.error("Se descarta un registro de Kafka sin metadatos", exception);
                    } else {
                        log.error("Se descarta el registro de Kafka (no recuperable o reintentos agotados) "
                                        + "(topic={}, partition={}, offset={}): {}",
                                record.topic(), record.partition(), record.offset(), exception.getMessage());
                    }
                },
                new FixedBackOff(properties.retryIntervalMs(), properties.retryMaxAttempts()));

        BackOff databaseBackOff =
                new FixedBackOff(properties.databaseRetryIntervalMs(), FixedBackOff.UNLIMITED_ATTEMPTS);
        errorHandler.setBackOffFunction((record, exception) -> {
            if (isDatabaseUnavailable(exception)) {
                log.warn("Base de datos no disponible; se reintentará el offset {} de la partición {} en {} ms",
                        record.offset(), record.partition(), properties.databaseRetryIntervalMs());
                return databaseBackOff;
            }
            return null; // null = usar el BackOff por defecto (reintentos limitados)
        });
        return errorHandler;
    }

    /**
     * Recorre la cadena de causas buscando un error de conexión o de
     * disponibilidad de la base de datos (no un error de los datos).
     */
    static boolean isDatabaseUnavailable(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof CannotCreateTransactionException
                    || current instanceof DataAccessResourceFailureException
                    || current instanceof TransientDataAccessException
                    || current instanceof RecoverableDataAccessException
                    || current instanceof SQLTransientConnectionException) {
                return true;
            }
            if (current.getCause() == current) {
                return false;
            }
            current = current.getCause();
        }
        return false;
    }
}
