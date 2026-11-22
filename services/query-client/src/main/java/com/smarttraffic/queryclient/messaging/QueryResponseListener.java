package com.smarttraffic.queryclient.messaging;

import com.smarttraffic.queryclient.dto.RouteQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class QueryResponseListener {

    private static final Logger log = LoggerFactory.getLogger(QueryResponseListener.class);

    @RabbitListener(queues = "#{clientAnswersQueue.name}")
    public void receive(RouteQueryResponse response, Message message) {
        log.info("{}", format(response));
    }

    String format(RouteQueryResponse response) {
        String status = response.routeStatus() == null
                ? response.result().name()
                : response.routeStatus().name();
        return "[%s] Trayecto [%s] → %s".formatted(
                response.userId(), String.join(", ", response.trayecto()), status);
    }
}
