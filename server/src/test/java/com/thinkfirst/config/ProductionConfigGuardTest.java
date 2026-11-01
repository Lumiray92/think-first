package com.thinkfirst.config;

import com.thinkfirst.auth.JwtProperties;
import com.thinkfirst.vault.ApiKeyProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionConfigGuardTest {

    @Test
    void rejectsDevelopmentDefaults() {
        JwtProperties jwt = new JwtProperties();
        jwt.setSecret(ProductionConfigGuard.DEV_JWT_SECRET);
        ApiKeyProperties apiKey = new ApiKeyProperties();
        apiKey.setMasterKey(ProductionConfigGuard.DEV_MASTER_KEY);

        assertThrows(IllegalStateException.class,
                () -> new ProductionConfigGuard(jwt, apiKey).afterPropertiesSet());
    }

    @Test
    void rejectsShortJwtSecret() {
        JwtProperties jwt = new JwtProperties();
        jwt.setSecret("too-short");
        ApiKeyProperties apiKey = new ApiKeyProperties();
        apiKey.setMasterKey("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");

        assertThrows(IllegalStateException.class,
                () -> new ProductionConfigGuard(jwt, apiKey).afterPropertiesSet());
    }

    @Test
    void rejectsWrongMasterKeyLength() {
        JwtProperties jwt = new JwtProperties();
        jwt.setSecret("a-production-jwt-secret-with-at-least-32-bytes");
        ApiKeyProperties apiKey = new ApiKeyProperties();
        apiKey.setMasterKey("c2hvcnQ=");

        assertThrows(IllegalStateException.class,
                () -> new ProductionConfigGuard(jwt, apiKey).afterPropertiesSet());
    }

    @Test
    void acceptsStrongValues() {
        JwtProperties jwt = new JwtProperties();
        jwt.setSecret("a-production-jwt-secret-with-at-least-32-bytes");
        ApiKeyProperties apiKey = new ApiKeyProperties();
        apiKey.setMasterKey("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");

        assertDoesNotThrow(() -> new ProductionConfigGuard(jwt, apiKey).afterPropertiesSet());
    }
}
