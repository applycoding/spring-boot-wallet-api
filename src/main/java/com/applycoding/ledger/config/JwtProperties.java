package com.applycoding.ledger.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledger.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
