package com.smarttraffic.alertdispatcher.messaging;

import com.smarttraffic.alertdispatcher.dto.TrafficUpdateEvent;
import com.smarttraffic.alertdispatcher.service.TrafficUpdateProcessor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TrafficUpdateListener {

    private final TrafficUpdateProcessor processor;

    public TrafficUpdateListener(TrafficUpdateProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = "#{trafficUpdatesQueue.name}")
    public void receive(TrafficUpdateEvent event) {
        processor.process(event);
    }
}
