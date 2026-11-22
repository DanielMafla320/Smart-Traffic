package com.smarttraffic.queryclient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.regex.Pattern;

@Configuration
public class RabbitConfiguration {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    @Bean
    TopicExchange queryAnswersExchange(
            @Value("${traffic.rabbitmq.answers-exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Queue clientAnswersQueue() {
        return new AnonymousQueue();
    }

    @Bean
    Binding clientAnswersBinding(
            @Qualifier("clientAnswersQueue") Queue queue,
            @Qualifier("queryAnswersExchange") TopicExchange exchange,
            @Value("${traffic.client.user-id}") String userId) {
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            throw new IllegalArgumentException(
                    "USER_ID must contain only letters, numbers, underscores, and hyphens");
        }
        return BindingBuilder.bind(queue).to(exchange).with("answer." + userId);
    }

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}
