package com.smarttraffic.dashboard.domain;

/**
 * Estados de tráfico publicados por traffic-density-processor.
 * El orden va de menor a mayor congestión.
 */
public enum TrafficStatus {
    DESPEJADA,
    MODERADA,
    CONGESTIONADA
}
