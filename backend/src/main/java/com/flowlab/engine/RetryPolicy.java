package com.flowlab.engine;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RetryPolicy {

    private final long initialDelaySeconds = 2;
    private final double multiplier = 2.0;
    private final long maxDelaySeconds = 60;
    private final double jitterFraction = 0.2; // +/- 20% jitter

    public Duration calculateDelay(int attemptCount) {
        if (attemptCount <= 0) {
            attemptCount = 1;
        }

        // Exponential backoff: base = initial * (multiplier ^ (attempt - 1))
        double baseDelaySeconds = initialDelaySeconds * Math.pow(multiplier, attemptCount - 1);
        if (baseDelaySeconds > maxDelaySeconds) {
            baseDelaySeconds = maxDelaySeconds;
        }

        // Full / decorrelated jitter
        double jitterRange = baseDelaySeconds * jitterFraction;
        double jitter = ThreadLocalRandom.current().nextDouble(-jitterRange, jitterRange);
        long finalMillis = Math.max(500, (long) ((baseDelaySeconds + jitter) * 1000));

        return Duration.ofMillis(finalMillis);
    }

    public boolean canRetry(int attemptCount, int maxAttempts) {
        return attemptCount < maxAttempts;
    }
}
