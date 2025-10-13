package dev.diar.app.service.fakes;

import dev.diar.app.port.ClockPort;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public class FakeClock implements ClockPort {
    private ZonedDateTime now;

    public FakeClock(ZonedDateTime now) {
        this.now = now;
    }

    public void setNow(ZonedDateTime now) {
        this.now = now;
    }

    @Override
    public ZonedDateTime now() {
        return now;
    }

    @Override
    public LocalDate today() {
        return now.toLocalDate();
    }
}
