package com.smarttraffic.dashboard.service;

import com.smarttraffic.dashboard.dto.DashboardUpdateResponse;

/**
 * Arma el mensaje completo (zonas + resumen) que se envía por WebSocket.
 */
public interface DashboardSnapshotService {

    DashboardUpdateResponse currentSnapshot();
}
