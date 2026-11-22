package com.smarttraffic.dashboard.config;

import com.smarttraffic.dashboard.websocket.TrafficWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registra el endpoint WebSocket {@code /ws/traffic}. El handshake HTTP
 * ({@code Upgrade: websocket} → {@code 101 Switching Protocols}) lo hace Spring;
 * solo se aceptan los orígenes configurados.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    public static final String TRAFFIC_ENDPOINT = "/ws/traffic";

    private final TrafficWebSocketHandler trafficWebSocketHandler;
    private final DashboardProperties properties;

    public WebSocketConfig(TrafficWebSocketHandler trafficWebSocketHandler, DashboardProperties properties) {
        this.trafficWebSocketHandler = trafficWebSocketHandler;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(trafficWebSocketHandler, TRAFFIC_ENDPOINT)
                .setAllowedOrigins(properties.allowedOriginsArray());
    }
}
