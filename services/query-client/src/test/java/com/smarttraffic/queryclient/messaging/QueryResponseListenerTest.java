package com.smarttraffic.queryclient.messaging;

import com.smarttraffic.queryclient.domain.CongestionStatus;
import com.smarttraffic.queryclient.domain.QueryResultStatus;
import com.smarttraffic.queryclient.dto.RouteQueryResponse;
import com.smarttraffic.queryclient.dto.ZoneStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryResponseListenerTest {

    @Test
    void formatsResponseAsAReadableRouteAndStatus() {
        RouteQueryResponse response = new RouteQueryResponse(
                "query-1", "client_456", List.of("Zona A", "Zona C"),
                QueryResultStatus.OK,
                List.of(
                        new ZoneStatus("Zona A", CongestionStatus.MODERADA),
                        new ZoneStatus("Zona C", CongestionStatus.CONGESTIONADA)),
                CongestionStatus.CONGESTIONADA, Instant.now(), Instant.now(), "ok");
        QueryResponseListener listener = new QueryResponseListener();

        assertEquals(
                "[client_456] Trayecto [Zona A, Zona C] → CONGESTIONADA",
                listener.format(response));

        listener.receive(response, new Message(new byte[0], new MessageProperties()));
    }
}
