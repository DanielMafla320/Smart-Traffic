package com.smarttraffic.density.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimpleAverageStrategyTest {

    private final SimpleAverageStrategy strategy = new SimpleAverageStrategy();

    @Test
    void calculaElPromedioAritmetico() {
        assertThat(strategy.calculate(List.of(10, 20, 30))).isEqualTo(20.0);
        assertThat(strategy.calculate(List.of(45))).isEqualTo(45.0);
        assertThat(strategy.calculate(List.of(0, 0, 0, 0))).isEqualTo(0.0);
    }

    @Test
    void rechazaUnaVentanaVaciaONula() {
        assertThatThrownBy(() -> strategy.calculate(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> strategy.calculate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaLecturasNulasONegativas() {
        assertThatThrownBy(() -> strategy.calculate(Arrays.asList(10, null, 30)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> strategy.calculate(List.of(10, -1, 30)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
