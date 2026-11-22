package com.smarttraffic.alertdispatcher.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
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

import java.util.Map;

@Configuration
public class RabbitConfiguration {

    @Bean
    FanoutExchange trafficUpdatesExchange(
            @Value("${traffic.rabbitmq.updates-exchange}") String exchangeName) {
        return new FanoutExchange(exchangeName, true, false);
    }

    @Bean
    Queue trafficUpdatesQueue() {
        return new AnonymousQueue();
    }

    @Bean
    Binding trafficUpdatesBinding(
            @Qualifier("trafficUpdatesQueue") Queue queue,
            @Qualifier("trafficUpdatesExchange") FanoutExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange);
    }

    @Bean
    Queue queryTrafficQueue(
            @Value("${traffic.rabbitmq.query-queue}") String queueName,
            @Value("${traffic.rabbitmq.dead-letter-exchange}") String deadLetterExchange,
            @Value("${traffic.rabbitmq.dead-letter-queue}") String deadLetterQueue) {
        return new Queue(queueName, true, false, false,
                Map.of("x-dead-letter-exchange", deadLetterExchange,
                        "x-dead-letter-routing-key", deadLetterQueue));
    }

    @Bean
    TopicExchange queryAnswersExchange(
            @Value("${traffic.rabbitmq.answers-exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    DirectExchange queryDeadLetterExchange(
            @Value("${traffic.rabbitmq.dead-letter-exchange}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    Queue queryDeadLetterQueue(
            @Value("${traffic.rabbitmq.dead-letter-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Binding queryDeadLetterBinding(
            @Qualifier("queryDeadLetterQueue") Queue queue,
            @Qualifier("queryDeadLetterExchange") DirectExchange exchange,
            @Value("${traffic.rabbitmq.dead-letter-queue}") String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
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
