package com.smarttraffic.dashboard.service;

import com.smarttraffic.dashboard.domain.TrafficStats;

/**
 * Calcula el resumen general del tráfico a partir del estado actual.
 */
public interface TrafficStatsService {

    TrafficStats currentStats();
}
