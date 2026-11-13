package com.smarttraffic.density.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarttraffic.density.domain.TrafficStatus;
import com.smarttraffic.density.domain.TrafficThresholds;
import org.junit.jupiter.api.Test;

class ZoneStateTest {

    private final TrafficThresholds thresholds = new TrafficThresholds(30.0, 60.0);

    @Test
    void elEstadoDespejadoSeMantienePorDebajoDelUmbralModerado() {
        ZoneState state = new DespejadaState();
        ZoneState result = state.evaluate(29.99, thresholds);

        assertThat(result).isSameAs(state);
        assertThat(result.status()).isEqualTo(TrafficStatus.DESPEJADA);
    }

    @Test
    void elEstadoDespejadoTransicionaAModeradoYCongestionado() {
        assertThat(new DespejadaState().evaluate(30.0, thresholds))
                .isInstanceOf(ModeradaState.class);
        assertThat(new DespejadaState().evaluate(60.0, thresholds))
                .isInstanceOf(CongestionadaState.class);
    }

    @Test
    void elEstadoModeradoSeMantieneDentroDeSuRango() {
        ZoneState state = new ModeradaState();

        assertThat(state.evaluate(30.0, thresholds)).isSameAs(state);
        assertThat(state.evaluate(59.99, thresholds)).isSameAs(state);
    }

    @Test
    void elEstadoModeradoTransicionaEnAmbosSentidos() {
        assertThat(new ModeradaState().evaluate(29.99, thresholds))
                .isInstanceOf(DespejadaState.class);
        assertThat(new ModeradaState().evaluate(60.0, thresholds))
                .isInstanceOf(CongestionadaState.class);
    }

    @Test
    void elEstadoCongestionadoSeMantieneEnOporEncimaDelUmbral() {
        ZoneState state = new CongestionadaState();

        assertThat(state.evaluate(60.0, thresholds)).isSameAs(state);
        assertThat(state.evaluate(80.0, thresholds)).isSameAs(state);
    }

    @Test
    void elEstadoCongestionadoTransicionaEnAmbosSentidos() {
        assertThat(new CongestionadaState().evaluate(30.0, thresholds))
                .isInstanceOf(ModeradaState.class);
        assertThat(new CongestionadaState().evaluate(29.99, thresholds))
                .isInstanceOf(DespejadaState.class);
    }

    @Test
    void losLimitesExactosPertenecenAlEstadoSuperior() {
        assertThat(new DespejadaState().evaluate(30.0, thresholds).status())
                .isEqualTo(TrafficStatus.MODERADA);
        assertThat(new ModeradaState().evaluate(60.0, thresholds).status())
                .isEqualTo(TrafficStatus.CONGESTIONADA);
    }

    @Test
    void cadaEstadoReportaSuNombreEnEspanol() {
        assertThat(new DespejadaState().status()).isEqualTo(TrafficStatus.DESPEJADA);
        assertThat(new ModeradaState().status()).isEqualTo(TrafficStatus.MODERADA);
        assertThat(new CongestionadaState().status()).isEqualTo(TrafficStatus.CONGESTIONADA);
    }

    @Test
    void rechazaUmbralesIncoherentes() {
        assertThatThrownBy(() -> new TrafficThresholds(60.0, 30.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrafficThresholds(30.0, 30.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrafficThresholds(-1.0, 30.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
