package com.smarttraffic.queryclient.messaging;

import com.smarttraffic.queryclient.dto.QueryRequestRecord;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TrafficQueryPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String requestQueue;

    public TrafficQueryPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${traffic.rabbitmq.request-queue}") String requestQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.requestQueue = requestQueue;
    }

    public void publish(QueryRequestRecord request) {
        MessagePostProcessor correlation = message ->
                setCorrelationId(message, UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend("", requestQueue, request, correlation);
    }

    private Message setCorrelationId(Message message, String correlationId) {
        message.getMessageProperties().setCorrelationId(correlationId);
        return message;
    }
}
