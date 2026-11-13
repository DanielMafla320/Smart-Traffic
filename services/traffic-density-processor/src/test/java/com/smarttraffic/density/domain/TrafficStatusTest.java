package com.smarttraffic.density.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TrafficStatusTest {

    @Test
    void contieneExclusivamenteLosEstadosEnEspanol() {
        assertThat(TrafficStatus.values()).containsExactly(
                TrafficStatus.DESPEJADA,
                TrafficStatus.MODERADA,
                TrafficStatus.CONGESTIONADA);
    }
}
