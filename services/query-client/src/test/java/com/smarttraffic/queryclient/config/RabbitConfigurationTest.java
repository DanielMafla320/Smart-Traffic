package com.smarttraffic.queryclient.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.TopicExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RabbitConfigurationTest {

    @Test
    void bindsClientQueueToTheSingularAnswerRoutingKey() {
        Binding binding = new RabbitConfiguration().clientAnswersBinding(
                new AnonymousQueue(), new TopicExchange("query_answers"), "client_456");

        assertEquals("answer.client_456", binding.getRoutingKey());
    }
}
