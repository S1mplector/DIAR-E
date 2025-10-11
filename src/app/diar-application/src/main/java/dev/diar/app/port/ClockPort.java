package dev.diar.app.port;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public interface ClockPort {
    ZonedDateTime now();
    LocalDate today();
}
