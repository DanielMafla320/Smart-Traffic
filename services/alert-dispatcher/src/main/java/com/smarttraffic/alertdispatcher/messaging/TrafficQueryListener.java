package com.smarttraffic.alertdispatcher.messaging;

import com.smarttraffic.alertdispatcher.dto.QueryRequestRecord;
import com.smarttraffic.alertdispatcher.dto.RouteQueryResponse;
import com.smarttraffic.alertdispatcher.service.TrafficQueryService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class TrafficQueryListener {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final TrafficQueryService queryService;
    private final RabbitTemplate rabbitTemplate;
    private final String answersExchange;

    public TrafficQueryListener(
            TrafficQueryService queryService,
            RabbitTemplate rabbitTemplate,
            @Value("${traffic.rabbitmq.answers-exchange}") String answersExchange) {
        this.queryService = queryService;
        this.rabbitTemplate = rabbitTemplate;
        this.answersExchange = answersExchange;
    }

    @RabbitListener(queues = "${traffic.rabbitmq.query-queue}")
    public void receive(QueryRequestRecord request, Message message) {
        if (request == null || !USER_ID_PATTERN.matcher(request.userId()).matches()) {
            throw new IllegalArgumentException("user_id cannot be used as a routing key");
        }

        String correlationId = message.getMessageProperties().getCorrelationId();
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        RouteQueryResponse response = queryService.resolve(request, correlationId);
        String replyCorrelationId = correlationId;
        MessagePostProcessor correlation = outgoing ->
                setCorrelationId(outgoing, replyCorrelationId);

        rabbitTemplate.convertAndSend(
                answersExchange, "answer." + request.userId(), response, correlation);
    }

    private Message setCorrelationId(Message message, String correlationId) {
        message.getMessageProperties().setCorrelationId(correlationId);
        return message;
    }
}
