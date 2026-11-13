package com.smarttraffic.density.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeightedAverageStrategyTest {

    private final WeightedAverageStrategy strategy = new WeightedAverageStrategy();

    @Test
    void aplicaMayorPesoALasLecturasRecientes() {
        double weighted = strategy.calculate(List.of(10, 20, 30));

        assertThat(weighted).isEqualTo(140.0 / 6.0);
        assertThat(weighted).isGreaterThan(new SimpleAverageStrategy().calculate(List.of(10, 20, 30)));
    }

    @Test
    void unaVentanaDeUnaSolaLecturaDevuelveEseValor() {
        assertThat(strategy.calculate(List.of(45))).isEqualTo(45.0);
    }

    @Test
    void unaLecturaRecienteAltaElevaElPromedioPonderado() {
        assertThat(strategy.calculate(List.of(0, 0, 100))).isEqualTo(50.0);
        assertThat(new SimpleAverageStrategy().calculate(List.of(0, 0, 100)))
                .isEqualTo(100.0 / 3.0);
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
