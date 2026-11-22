package com.smarttraffic.dashboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de RabbitMQ del dashboard:
 *
 * <pre>
 * traffic-density-processor ──▶ [exchange fanout traffic_updates] ──binding──▶ [cola dashboard_traffic_queue] ──▶ dashboard
 * </pre>
 *
 * <p>El exchange se declara con los mismos parámetros que el procesador
 * (durable, sin auto-delete). Declararlo dos veces es seguro: RabbitMQ solo lo
 * crea si no existe. La cola y el binding son propios del dashboard.
 */
@Configuration(proxyBeanMethods = false)
public class RabbitConfig {

    @Bean
    public FanoutExchange trafficUpdatesExchange(DashboardProperties properties) {
        return new FanoutExchange(properties.exchange(), true, false);
    }

    /**
     * Cola durable: sobrevive a un reinicio de RabbitMQ. Como solo importa el
     * último estado, se limita su tamaño y se descartan los mensajes más viejos.
     */
    @Bean
    public Queue dashboardTrafficQueue(DashboardProperties properties) {
        return QueueBuilder.durable(properties.queue())
                .maxLength(properties.queueMaxLength())
                .build();
    }

    @Bean
    public Binding dashboardTrafficBinding(Queue dashboardTrafficQueue, FanoutExchange trafficUpdatesExchange) {
        // En un fanout la routing key se ignora: llega una copia de cada mensaje.
        return BindingBuilder.bind(dashboardTrafficQueue).to(trafficUpdatesExchange);
    }

    /**
     * El procesador no envía la cabecera __TypeId__, así que el JSON se
     * convierte siempre al tipo del parámetro del {@code @RabbitListener}.
     */
    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
