package com.smarttraffic.queryclient.service;

import com.smarttraffic.queryclient.dto.QueryRequestRecord;
import com.smarttraffic.queryclient.messaging.TrafficQueryPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledQueryProducerTest {

    @Mock
    private TrafficQueryPublisher publisher;

    @Test
    void alternatesSingleZoneAndMultiZoneQueriesWithTheConfiguredUserId() {
        ScheduledQueryProducer producer =
                new ScheduledQueryProducer(publisher, "client_2", "Zona B", "Zona E");

        producer.sendNextQuery();
        producer.sendNextQuery();

        ArgumentCaptor<QueryRequestRecord> requests =
                ArgumentCaptor.forClass(QueryRequestRecord.class);
        verify(publisher, org.mockito.Mockito.times(2)).publish(requests.capture());

        assertEquals("client_2", requests.getAllValues().get(0).userId());
        assertEquals(java.util.List.of("Zona B"), requests.getAllValues().get(0).trayecto());
        assertEquals(java.util.List.of("Zona B", "Zona E"),
                requests.getAllValues().get(1).trayecto());
    }
}
