package com.smarttraffic.density.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttraffic.density.config.DensityProperties.Strategy;
import com.smarttraffic.density.strategy.DensityStrategy;
import com.smarttraffic.density.strategy.SimpleAverageStrategy;
import com.smarttraffic.density.strategy.WeightedAverageStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DensityStrategyConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DensityStrategyConfig.class);

    @Test
    void seleccionaElPromedioSimplePorConfiguracion() {
        runner.withBean(DensityProperties.class,
                        () -> new DensityProperties(Strategy.SIMPLE_AVERAGE, 10, 30.0, 60.0))
                .run(context -> assertThat(context.getBean(DensityStrategy.class))
                        .isInstanceOf(SimpleAverageStrategy.class));
    }

    @Test
    void seleccionaElPromedioPonderadoPorConfiguracion() {
        runner.withBean(DensityProperties.class,
                        () -> new DensityProperties(Strategy.WEIGHTED_AVERAGE, 10, 30.0, 60.0))
                .run(context -> assertThat(context.getBean(DensityStrategy.class))
                        .isInstanceOf(WeightedAverageStrategy.class));
    }
}
