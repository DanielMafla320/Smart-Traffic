package com.smarttraffic.sensor.simulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.smarttraffic.sensor.config.SimulatorProperties;
import com.smarttraffic.sensor.producer.TrafficSensorProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class TrafficSimulationServiceConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class)
            .withBean(SensorReadingGenerator.class, SensorReadingGenerator::new)
            .withBean(TrafficSensorProducer.class, () -> mock(TrafficSensorProducer.class));

    @Test
    void creaElServicioCuandoEstaHabilitado() {
        runner.withPropertyValues(validProperties())
                .run(context -> assertThat(context).hasSingleBean(TrafficSimulationService.class));
    }

    @Test
    void noCreaElServicioCuandoEstaDeshabilitado() {
        runner.withPropertyValues(validProperties())
                .withPropertyValues("smarttraffic.simulator.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(TrafficSimulationService.class));
    }

    private String[] validProperties() {
        return new String[] {
                "smarttraffic.simulator.zones=Zona A",
                "smarttraffic.simulator.vehicle-count-min=0",
                "smarttraffic.simulator.vehicle-count-max=120",
                "smarttraffic.simulator.interval-ms=2000"
        };
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SimulatorProperties.class)
    @Import(TrafficSimulationService.class)
    static class TestConfig {
    }
}
