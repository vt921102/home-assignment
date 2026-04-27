package com.toanlv.flashsale.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    /**
     * Application-wide Clock bean.
     *
     * Why inject Clock instead of calling Instant.now() directly:
     *   - Tests can construct services with Clock.fixed(...) to control
     *     time deterministically — no Thread.sleep(), no flaky timing.
     *   - Every time-sensitive service (OtpService, PurchaseService,
     *     JwtService, RefreshTokenService) accepts Clock via constructor,
     *     making them fully testable without mocking static methods.
     *
     * Timezone Asia/Ho_Chi_Minh:
     *   Flash sale sessions store start_time and end_time as LocalTime.
     *   The server must interpret "08:00" as 08:00 Vietnam time, not UTC.
     *   All LocalDate.now(clock) and LocalTime.now(clock) calls will
     *   return the correct local time for Vietnamese users.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
