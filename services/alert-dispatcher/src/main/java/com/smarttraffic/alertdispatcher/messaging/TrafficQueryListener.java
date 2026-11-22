package com.smarttraffic.alertdispatcher.messaging;

import com.smarttraffic.alertdispatcher.dto.RouteQueryRequest;
import com.smarttraffic.alertdispatcher.dto.RouteQueryResponse;
import com.smarttraffic.alertdispatcher.service.TrafficQueryService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class TrafficQueryListener {

    private static final Pattern CLIENT_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

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
    public void receive(RouteQueryRequest request, Message message) {
        RouteQueryResponse response = queryService.resolve(request);
        if (request == null || request.clientId() == null
                || !CLIENT_ID_PATTERN.matcher(request.clientId()).matches()) {
            throw new IllegalArgumentException("Query clientId cannot be used as a routing key");
        }

        String correlationId = message.getMessageProperties().getCorrelationId();
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.queryId();
        }
        String replyCorrelationId = correlationId;
        MessagePostProcessor correlation = outgoing ->
                setCorrelationId(outgoing, replyCorrelationId);

        rabbitTemplate.convertAndSend(
                answersExchange, "answers." + request.clientId(), response, correlation);
    }

    private Message setCorrelationId(Message message, String correlationId) {
        message.getMessageProperties().setCorrelationId(correlationId);
        return message;
    }
}
