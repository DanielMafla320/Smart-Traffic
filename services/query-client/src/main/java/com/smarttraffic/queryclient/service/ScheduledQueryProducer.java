package com.smarttraffic.queryclient.service;

import com.smarttraffic.queryclient.domain.QueryType;
import com.smarttraffic.queryclient.domain.TrafficZone;
import com.smarttraffic.queryclient.dto.RouteQueryRequest;
import com.smarttraffic.queryclient.messaging.TrafficQueryPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ScheduledQueryProducer {

    private final TrafficQueryPublisher publisher;
    private final String clientId;
    private final TrafficZone zone;
    private final TrafficZone destinationZone;
    private final AtomicLong queryCount = new AtomicLong();

    public ScheduledQueryProducer(
            TrafficQueryPublisher publisher,
            @Value("${traffic.client.id}") String clientId,
            @Value("${traffic.client.zone}") TrafficZone zone,
            @Value("${traffic.client.destination-zone}") TrafficZone destinationZone) {
        this.publisher = publisher;
        this.clientId = clientId;
        this.zone = zone;
        this.destinationZone = destinationZone;
    }

    @Scheduled(
            fixedRateString = "${traffic.client.query-interval-ms:15000}",
            initialDelayString = "${traffic.client.query-interval-ms:15000}")
    public void sendNextQuery() {
        long currentCount = queryCount.getAndIncrement();
        String queryId = UUID.randomUUID().toString();
        RouteQueryRequest request = currentCount % 2 == 0
                ? new RouteQueryRequest(queryId, clientId, QueryType.ZONE,
                        zone, null, null, Instant.now())
                : new RouteQueryRequest(queryId, clientId, QueryType.ROUTE,
                        null, zone, destinationZone, Instant.now());
        publisher.publish(request);
    }
}
