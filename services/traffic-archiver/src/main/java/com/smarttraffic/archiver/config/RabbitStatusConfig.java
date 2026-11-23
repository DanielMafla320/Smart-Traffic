package com.smarttraffic.archiver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Suscripción del archivador al exchange fanout {@code traffic_updates}.
 *
 * <p>La cola es durable y con nombre fijo (no anónima): si el archivador se
 * detiene, los snapshots se acumulan y se procesan al volver, así el historial
 * de estados no tiene huecos.
 */
@Configuration(proxyBeanMethods = false)
public class RabbitStatusConfig {

    @Bean
    public FanoutExchange trafficUpdatesExchange(
            @Value("${smarttraffic.rabbit.exchange:traffic_updates}") String exchangeName) {
        return new FanoutExchange(exchangeName, true, false);
    }

    @Bean
    public Queue trafficStatusHistoryQueue(
            @Value("${smarttraffic.rabbit.status-history-queue:traffic_status_history_queue}")
            String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding trafficStatusHistoryBinding(
            Queue trafficStatusHistoryQueue, FanoutExchange trafficUpdatesExchange) {
        return BindingBuilder.bind(trafficStatusHistoryQueue).to(trafficUpdatesExchange);
    }

    /**
     * El procesador no envía la cabecera {@code __TypeId__}; el conversor usa el
     * tipo del parámetro del listener ({@code TrafficUpdateMessage}).
     */
    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
