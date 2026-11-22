package com.smarttraffic.queryclient.messaging;

import com.smarttraffic.queryclient.dto.QueryRequestRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrafficQueryPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void sendsTheRequestBodyToTheConfiguredQueryQueueWithCorrelationId() {
        QueryRequestRecord request = new QueryRequestRecord(
                "client_456", List.of("Zona A", "Zona C"));

        new TrafficQueryPublisher(rabbitTemplate, "query_traffic_queue").publish(request);

        ArgumentCaptor<QueryRequestRecord> body =
                ArgumentCaptor.forClass(QueryRequestRecord.class);
        ArgumentCaptor<MessagePostProcessor> postProcessor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(""), eq("query_traffic_queue"), body.capture(), postProcessor.capture());

        assertEquals(request, body.getValue());
        Message outgoing = postProcessor.getValue().postProcessMessage(
                new Message(new byte[0], new MessageProperties()));
        assertNotNull(outgoing.getMessageProperties().getCorrelationId());
    }
}
