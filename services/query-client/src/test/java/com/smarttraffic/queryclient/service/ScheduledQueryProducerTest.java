package com.smarttraffic.queryclient.service;

import com.smarttraffic.queryclient.domain.QueryType;
import com.smarttraffic.queryclient.domain.TrafficZone;
import com.smarttraffic.queryclient.dto.RouteQueryRequest;
import com.smarttraffic.queryclient.messaging.TrafficQueryPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledQueryProducerTest {

    @Mock
    private TrafficQueryPublisher publisher;

    @Test
    void alternatesZoneAndRouteQueriesWithTheConfiguredClientId() {
        ScheduledQueryProducer producer =
                new ScheduledQueryProducer(publisher, "client-2", TrafficZone.B, TrafficZone.E);

        producer.sendNextQuery();
        producer.sendNextQuery();

        ArgumentCaptor<RouteQueryRequest> requests =
                ArgumentCaptor.forClass(RouteQueryRequest.class);
        verify(publisher, org.mockito.Mockito.times(2)).publish(requests.capture());

        assertEquals(QueryType.ZONE, requests.getAllValues().get(0).type());
        assertEquals(QueryType.ROUTE, requests.getAllValues().get(1).type());
        assertEquals("client-2", requests.getAllValues().get(0).clientId());
        assertEquals(TrafficZone.B, requests.getAllValues().get(1).origin());
        assertEquals(TrafficZone.E, requests.getAllValues().get(1).destination());
        assertNotNull(requests.getAllValues().get(0).queryId());
    }
}
