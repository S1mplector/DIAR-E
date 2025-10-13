package dev.diar.app.service;

import dev.diar.app.port.ClockPort;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public class SystemClock implements ClockPort {
    @Override
    public ZonedDateTime now() {
        return ZonedDateTime.now();
    }

    @Override
    public LocalDate today() {
        return LocalDate.now();
    }
}
