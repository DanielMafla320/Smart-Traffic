package com.smarttraffic.density.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.density.domain.TrafficReadingEvent;
import java.util.Map;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Configuración del consumidor Kafka.
 *
 * <p>{@link ErrorHandlingDeserializer} evita que un JSON mal formado detenga el
 * contenedor. {@link DefaultErrorHandler} limita los reintentos de errores
 * inesperados y finalmente descarta el registro con un log.
 */
@Configuration(proxyBeanMethods = false)
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public ConsumerFactory<String, TrafficReadingEvent> trafficReadingConsumerFactory(
            KafkaProperties kafkaProperties,
            ObjectProvider<SslBundles> sslBundles,
            ObjectMapper objectMapper) {
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties(sslBundles.getIfAvailable());

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
            @Value("${spring.kafka.listener.concurrency:3}") int concurrency) {
        ConcurrentKafkaListenerContainerFactory<String, TrafficReadingEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(concurrency);
        factory.setCommonErrorHandler(kafkaCommonErrorHandler);
        factory.setMissingTopicsFatal(false);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public CommonErrorHandler kafkaCommonErrorHandler(
            @Value("${smarttraffic.kafka.retry-interval-ms:1000}") long retryIntervalMs,
            @Value("${smarttraffic.kafka.retry-max-attempts:2}") long retryMaxAttempts) {
        return new DefaultErrorHandler(
                (record, exception) -> {
                    if (record == null) {
                        log.error("Se descarta un registro de Kafka sin metadatos tras agotar reintentos",
                                exception);
                    } else {
                        log.error("Se descarta el registro de Kafka tras agotar reintentos "
                                        + "(topic={}, partition={}, offset={})",
                                record.topic(), record.partition(), record.offset(), exception);
                    }
                },
                new FixedBackOff(retryIntervalMs, retryMaxAttempts));
    }
}
