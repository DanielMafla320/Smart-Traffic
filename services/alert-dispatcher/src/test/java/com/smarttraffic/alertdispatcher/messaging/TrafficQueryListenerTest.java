package com.smarttraffic.alertdispatcher.messaging;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.domain.QueryResultStatus;
import com.smarttraffic.alertdispatcher.dto.QueryRequestRecord;
import com.smarttraffic.alertdispatcher.dto.RouteQueryResponse;
import com.smarttraffic.alertdispatcher.dto.ZoneStatus;
import com.smarttraffic.alertdispatcher.service.TrafficQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrafficQueryListenerTest {

    @Mock
    private TrafficQueryService queryService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publishesAnswerToTheUserRoutingKey() {
        QueryRequestRecord request = new QueryRequestRecord(
                "client_456", List.of("Zona A", "Zona C"));
        RouteQueryResponse response = new RouteQueryResponse(
                "query-1", "client_456", request.trayecto(), QueryResultStatus.OK,
                List.of(new ZoneStatus("Zona A", CongestionStatus.MODERADA)),
                CongestionStatus.MODERADA, Instant.now(), Instant.now(), "ok");
        when(queryService.resolve(request, "query-1")).thenReturn(response);

        MessageProperties properties = new MessageProperties();
        properties.setCorrelationId("query-1");
        Message message = new Message(new byte[0], properties);
        new TrafficQueryListener(queryService, rabbitTemplate, "query_answers")
                .receive(request, message);

        verify(rabbitTemplate).convertAndSend(
                eq("query_answers"), eq("answer.client_456"), eq(response),
                any(MessagePostProcessor.class));
    }
}
