package com.smarttraffic.queryclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record QueryRequestRecord(
        @JsonProperty("user_id") String userId,
        @JsonProperty("trayecto") List<String> trayecto) {

    public QueryRequestRecord {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user_id cannot be blank");
        }
        userId = userId.trim();
        if (trayecto == null || trayecto.isEmpty()) {
            throw new IllegalArgumentException("trayecto cannot be empty");
        }
        if (trayecto.stream().anyMatch(zone -> zone == null)) {
            throw new IllegalArgumentException("trayecto cannot contain null zones");
        }
        trayecto = trayecto.stream().map(String::trim).toList();
        if (trayecto.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("trayecto cannot contain blank zones");
        }
        trayecto = List.copyOf(trayecto);
    }
}
