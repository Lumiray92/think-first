package com.thinkfirst.auth.dto;

import java.time.Instant;

/** 只暴露掩码，绝不包含 API Key 明文。 */
public record UserView(
        Long id,
        String email,
        boolean hasApiKey,
        String apiKeyMask,
        Instant apiKeyUpdatedAt
) {
}
