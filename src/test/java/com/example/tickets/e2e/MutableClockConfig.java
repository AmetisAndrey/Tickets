package com.example.tickets.e2e;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Подменяет продовый Clock.systemUTC() на управляемый.
 * Тест может двигать время через MutableClock.set(...).
 */
@TestConfiguration
public class MutableClockConfig {

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return new MutableClock(Instant.parse("2025-01-15T12:00:00Z"), ZoneOffset.UTC);
    }

    public static class MutableClock extends Clock {
        private volatile Instant now;
        private final ZoneOffset zone;

        public MutableClock(Instant now, ZoneOffset zone) {
            this.now = now;
            this.zone = zone;
        }

        public void set(Instant instant) { this.now = instant; }
        public void plusSeconds(long s) { this.now = this.now.plusSeconds(s); }

        @Override public ZoneOffset getZone() { return zone; }
        @Override public Clock withZone(java.time.ZoneId z) { return this; }
        @Override public Instant instant() { return now; }
    }
}