package com.smarttraffic.queryclient.service;

import com.smarttraffic.queryclient.dto.QueryRequestRecord;
import com.smarttraffic.queryclient.messaging.TrafficQueryPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ScheduledQueryProducer {

    private final TrafficQueryPublisher publisher;
    private final String userId;
    private final String zone;
    private final String destinationZone;
    private final AtomicLong queryCount = new AtomicLong();

    public ScheduledQueryProducer(
            TrafficQueryPublisher publisher,
            @Value("${traffic.client.user-id}") String userId,
            @Value("${traffic.client.zone}") String zone,
            @Value("${traffic.client.destination-zone}") String destinationZone) {
        this.publisher = publisher;
        this.userId = userId;
        this.zone = zone;
        this.destinationZone = destinationZone;
    }

    @Scheduled(
            fixedRateString = "${traffic.client.query-interval-ms:15000}",
            initialDelayString = "${traffic.client.query-interval-ms:15000}")
    public void sendNextQuery() {
        long currentCount = queryCount.getAndIncrement();
        QueryRequestRecord request = currentCount % 2 == 0
                ? new QueryRequestRecord(userId, List.of(zone))
                : new QueryRequestRecord(userId, List.of(zone, destinationZone));
        publisher.publish(request);
    }
}
