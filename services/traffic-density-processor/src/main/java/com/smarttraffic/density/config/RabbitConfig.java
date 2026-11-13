package com.smarttraffic.density.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara el exchange fanout y el conversor JSON usado por RabbitTemplate.
 *
 * <p>Spring Boot detecta el {@link MessageConverter} y lo aplica al
 * {@code RabbitTemplate} y al {@code RabbitAdmin} autoconfigurados. El exchange
 * durable se declara al establecer conexión con el broker.
 *
 * <p>El conversor no añade la cabecera {@code __TypeId__}: el contrato es el JSON
 * y los consumidores de otros microservicios no necesitan conocer las clases Java
 * del procesador.
 */
@Configuration(proxyBeanMethods = false)
public class RabbitConfig {

    private static final ClassMapper NO_TYPE_INFO = new ClassMapper() {
        @Override
        public void fromClass(Class<?> clazz, MessageProperties properties) {
            // Intencionadamente vacío: no se escribe la cabecera __TypeId__.
        }

        @Override
        public Class<?> toClass(MessageProperties properties) {
            return null;
        }
    };

    @Bean
    public FanoutExchange trafficUpdatesExchange(RabbitMessagingProperties properties) {
        return new FanoutExchange(properties.exchange(), true, false);
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setClassMapper(NO_TYPE_INFO);
        return converter;
    }
}
