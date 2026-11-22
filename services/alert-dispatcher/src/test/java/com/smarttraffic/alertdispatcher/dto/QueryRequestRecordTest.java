package com.smarttraffic.alertdispatcher.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryRequestRecordTest {

    @Test
    void serializesTheContractFieldNames() throws Exception {
        QueryRequestRecord request = new QueryRequestRecord(
                "client_456", List.of("Zona A", "Zona C"));

        assertEquals(
                "{\"user_id\":\"client_456\",\"trayecto\":[\"Zona A\",\"Zona C\"]}",
                new ObjectMapper().writeValueAsString(request));
    }

    @Test
    void rejectsBlankUserIdsEmptyRoutesAndNullZones() {
        assertThrows(IllegalArgumentException.class,
                () -> new QueryRequestRecord("  ", List.of("Zona A")));
        assertThrows(IllegalArgumentException.class,
                () -> new QueryRequestRecord("client_456", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new QueryRequestRecord("client_456", Arrays.asList("Zona A", null)));
    }
}
