package com.thinkfirst.vault.dto;

import java.time.Instant;

/** 只返回 `sk-****abcd` 形式的掩码。 */
public record ApiKeyView(String apiKeyMask, boolean hasApiKey, Instant updatedAt) {
}
