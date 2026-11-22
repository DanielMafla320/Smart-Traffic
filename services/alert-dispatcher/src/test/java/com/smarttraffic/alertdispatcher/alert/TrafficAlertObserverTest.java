package com.smarttraffic.alertdispatcher.alert;

import com.smarttraffic.alertdispatcher.domain.CongestionStatus;
import com.smarttraffic.alertdispatcher.state.StatusChange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class TrafficAlertObserverTest {

    @Test
    void criticalAlertIsLoggedOnlyForCongestedStatus(CapturedOutput output) {
        TrafficAlertSubject subject =
                new TrafficAlertSubject(List.of(new CriticalCongestionObserver()));

        subject.publish(change(CongestionStatus.MODERADA, CongestionStatus.DESPEJADA));
        subject.publish(change(
                CongestionStatus.CONGESTIONADA, CongestionStatus.CONGESTIONADA));
        assertThat(output).doesNotContain("¡La Zona A ahora está congestionada!");

        subject.publish(change(CongestionStatus.DESPEJADA, CongestionStatus.CONGESTIONADA));
        assertThat(output).contains("¡La Zona A ahora está congestionada!");
    }

    @Test
    void consoleAlertUsesTheSpanishStatusMessage(CapturedOutput output) {
        new ConsoleAlertObserver().onStatusChange(
                change(CongestionStatus.MODERADA, CongestionStatus.DESPEJADA));

        assertThat(output).contains("Zona A cambió de MODERADA a DESPEJADA");
    }

    private StatusChange change(
            CongestionStatus previous, CongestionStatus current) {
        return new StatusChange("Zona A", previous, current, Instant.EPOCH);
    }
}
