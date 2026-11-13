package com.smarttraffic.density.config;

import com.smarttraffic.density.strategy.DensityStrategy;
import com.smarttraffic.density.strategy.SimpleAverageStrategy;
import com.smarttraffic.density.strategy.WeightedAverageStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selecciona la estrategia de densidad según {@code smarttraffic.density.strategy}.
 */
@Configuration(proxyBeanMethods = false)
public class DensityStrategyConfig {

    @Bean
    public DensityStrategy densityStrategy(DensityProperties properties) {
        return switch (properties.strategy()) {
            case SIMPLE_AVERAGE -> new SimpleAverageStrategy();
            case WEIGHTED_AVERAGE -> new WeightedAverageStrategy();
        };
    }
}
