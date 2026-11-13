package com.smarttraffic.sensor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.sensor.domain.SensorReadingRecord;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Configuración tipada del productor Kafka y del topic de lecturas.
 *
 * <p>La serialización JSON usa el {@link ObjectMapper} de Spring Boot para que
 * {@link java.time.Instant} se escriba como ISO-8601 UTC. Se desactivan las
 * cabeceras de tipo de Spring Kafka para que el contrato no dependa de nombres
 * de clases Java del productor.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, SensorReadingRecord> sensorReadingProducerFactory(
            KafkaProperties kafkaProperties,
            ObjectProvider<SslBundles> sslBundles,
            ObjectMapper objectMapper) {
        Map<String, Object> properties = kafkaProperties.buildProducerProperties(sslBundles.getIfAvailable());

        JsonSerializer<SensorReadingRecord> valueSerializer = new JsonSerializer<>(objectMapper);
        valueSerializer.setAddTypeInfo(false);

        return new DefaultKafkaProducerFactory<>(
                properties,
                new StringSerializer(),
                valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, SensorReadingRecord> sensorReadingKafkaTemplate(
            ProducerFactory<String, SensorReadingRecord> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic rawTrafficDataTopic(
            @Value("${smarttraffic.kafka.topic:raw_traffic_data}") String topic,
            @Value("${smarttraffic.kafka.partitions:3}") int partitions,
            @Value("${smarttraffic.kafka.replicas:1}") short replicas) {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
